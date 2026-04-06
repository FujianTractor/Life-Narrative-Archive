from __future__ import annotations

import base64
import hashlib
import hmac
import json
import mimetypes
import os
import re
import secrets
import shutil
import sqlite3
import uuid
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any

import jwt
import numpy as np
from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from langchain_community.vectorstores import FAISS
from langchain_core.documents import Document
from langchain_core.embeddings import Embeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pydantic import BaseModel, ConfigDict, Field, field_validator
from starlette.staticfiles import StaticFiles


BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
UPLOADS_DIR = BASE_DIR / "uploads"
RAG_DIR = DATA_DIR / "rag"
ARCHIVES_PATH = DATA_DIR / "elders.json"
DB_PATH = DATA_DIR / "app.db"
STATIC_FILES = {"index.html", "styles.css", "script.js"}
TOKEN_EXPIRE_HOURS = 12
JWT_SECRET = os.getenv("ELDER_ARCHIVE_JWT_SECRET", "elder-life-archive-dev-secret")
JWT_ALGORITHM = "HS256"
PASSWORD_ITERATIONS = 120_000
EMBED_DIMENSIONS = 384

DATA_DIR.mkdir(parents=True, exist_ok=True)
UPLOADS_DIR.mkdir(parents=True, exist_ok=True)
RAG_DIR.mkdir(parents=True, exist_ok=True)

bearer_scheme = HTTPBearer(auto_error=False)
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=320,
    chunk_overlap=60,
    separators=["\n\n", "\n", "。", "！", "？", "；", "，", " "],
)


class UserRegisterPayload(BaseModel):
    username: str
    password: str
    display_name: str | None = Field(default=None, alias="displayName")

    model_config = ConfigDict(populate_by_name=True)

    @field_validator("username")
    @classmethod
    def validate_username(cls, value: str) -> str:
        cleaned = value.strip().lower()
        if not re.fullmatch(r"[a-z0-9_\-]{3,24}", cleaned):
            raise ValueError("用户名仅支持 3-24 位小写字母、数字、下划线或中划线。")
        return cleaned

    @field_validator("password")
    @classmethod
    def validate_password(cls, value: str) -> str:
        if len(value.strip()) < 6:
            raise ValueError("密码至少需要 6 位。")
        return value

    @field_validator("display_name")
    @classmethod
    def validate_display_name(cls, value: str | None) -> str | None:
        if value is None:
            return value
        cleaned = value.strip()
        return cleaned[:24] if cleaned else None


class UserLoginPayload(BaseModel):
    username: str
    password: str

    @field_validator("username")
    @classmethod
    def normalize_username(cls, value: str) -> str:
        return value.strip().lower()


class AssetPayload(BaseModel):
    name: str
    type: str
    size: int
    data_url: str = Field(alias="dataUrl")

    model_config = ConfigDict(populate_by_name=True)

    @field_validator("name")
    @classmethod
    def validate_name(cls, value: str) -> str:
        cleaned = value.strip()
        if not cleaned:
            raise ValueError("素材文件名不能为空。")
        return cleaned[:120]

    @field_validator("type")
    @classmethod
    def validate_type(cls, value: str) -> str:
        cleaned = value.strip().lower()
        if "/" not in cleaned:
            raise ValueError("素材类型无效。")
        return cleaned


class ArchiveCreatePayload(BaseModel):
    name: str
    age: int
    hometown: str = ""
    community: str = ""
    role: str = ""
    summary: str = ""
    wish: str = ""
    tags: list[str] = Field(default_factory=list)
    supporters: list[str] = Field(default_factory=list)
    tone: str = "amber"
    images: list[AssetPayload] = Field(default_factory=list)
    videos: list[AssetPayload] = Field(default_factory=list)

    @field_validator("name")
    @classmethod
    def validate_name(cls, value: str) -> str:
        cleaned = value.strip()
        if not cleaned:
            raise ValueError("姓名不能为空。")
        return cleaned[:24]

    @field_validator("age")
    @classmethod
    def validate_age(cls, value: int) -> int:
        if value < 50 or value > 110:
            raise ValueError("年龄需在 50 到 110 岁之间。")
        return value


class TimelinePayload(BaseModel):
    year: str
    title: str
    description: str = ""

    @field_validator("year", "title", "description")
    @classmethod
    def trim_text(cls, value: str) -> str:
        return value.strip()


class AssetUploadRequest(BaseModel):
    images: list[AssetPayload] = Field(default_factory=list)
    videos: list[AssetPayload] = Field(default_factory=list)


class RagAskRequest(BaseModel):
    archive_id: str = Field(alias="archiveId")
    question: str

    model_config = ConfigDict(populate_by_name=True)

    @field_validator("archive_id", "question")
    @classmethod
    def trim_field(cls, value: str) -> str:
        cleaned = value.strip()
        if not cleaned:
            raise ValueError("问题和档案编号不能为空。")
        return cleaned


class RagRebuildRequest(BaseModel):
    archive_id: str | None = Field(default=None, alias="archiveId")

    model_config = ConfigDict(populate_by_name=True)


class HashEmbeddings(Embeddings):
    def __init__(self, dimensions: int = EMBED_DIMENSIONS) -> None:
        self.dimensions = dimensions

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        return [self._embed(text) for text in texts]

    def embed_query(self, text: str) -> list[float]:
        return self._embed(text)

    def _embed(self, text: str) -> list[float]:
        tokens = re.findall(r"[A-Za-z0-9_]+|[\u4e00-\u9fff]", text.lower())
        vector = np.zeros(self.dimensions, dtype=np.float32)
        if not tokens:
            return vector.tolist()

        for token in tokens:
            digest = hashlib.blake2b(token.encode("utf-8"), digest_size=16).digest()
            for index in range(0, 16, 4):
                bucket = int.from_bytes(digest[index:index + 4], "little") % self.dimensions
                sign = 1.0 if digest[index] % 2 == 0 else -1.0
                vector[bucket] += sign

        norm = float(np.linalg.norm(vector))
        if norm > 0:
            vector /= norm
        return vector.tolist()


embeddings = HashEmbeddings()
app = FastAPI(title="Elder Life Archive API", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.mount("/uploads", StaticFiles(directory=UPLOADS_DIR), name="uploads")


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    return JSONResponse(status_code=exc.status_code, content={"message": exc.detail})


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception) -> JSONResponse:
    return JSONResponse(status_code=500, content={"message": str(exc) or "服务器异常。"})


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def iso_now() -> str:
    return utc_now().replace(microsecond=0).isoformat()


def init_storage() -> None:
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    UPLOADS_DIR.mkdir(parents=True, exist_ok=True)
    RAG_DIR.mkdir(parents=True, exist_ok=True)
    if not ARCHIVES_PATH.exists():
        ARCHIVES_PATH.write_text(
            json.dumps({"updatedAt": iso_now(), "elders": []}, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )


def init_database() -> None:
    with sqlite3.connect(DB_PATH) as connection:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                username TEXT NOT NULL UNIQUE,
                display_name TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """
        )
        connection.commit()


def get_db() -> sqlite3.Connection:
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    return connection


def hash_password(password: str, salt: bytes | None = None) -> str:
    salt = salt or secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PASSWORD_ITERATIONS)
    return f"{salt.hex()}${digest.hex()}"


def verify_password(password: str, stored_value: str) -> bool:
    try:
        salt_hex, digest_hex = stored_value.split("$", 1)
        expected = bytes.fromhex(digest_hex)
        actual = hashlib.pbkdf2_hmac(
            "sha256",
            password.encode("utf-8"),
            bytes.fromhex(salt_hex),
            PASSWORD_ITERATIONS,
        )
        return hmac.compare_digest(actual, expected)
    except ValueError:
        return False


def create_access_token(user: dict[str, Any]) -> str:
    payload = {
        "sub": user["id"],
        "username": user["username"],
        "displayName": user["display_name"],
        "exp": utc_now() + timedelta(hours=TOKEN_EXPIRE_HOURS),
    }
    return jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGORITHM)


def fetch_user_by_id(user_id: str) -> dict[str, Any] | None:
    with get_db() as connection:
        row = connection.execute(
            "SELECT id, username, display_name, created_at FROM users WHERE id = ?",
            (user_id,),
        ).fetchone()
    return dict(row) if row else None


def fetch_user_by_username(username: str) -> dict[str, Any] | None:
    with get_db() as connection:
        row = connection.execute(
            "SELECT id, username, display_name, password_hash, created_at FROM users WHERE username = ?",
            (username,),
        ).fetchone()
    return dict(row) if row else None


def ensure_authenticated(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
) -> dict[str, Any]:
    if credentials is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="请先登录。")

    try:
        payload = jwt.decode(credentials.credentials, JWT_SECRET, algorithms=[JWT_ALGORITHM])
    except jwt.InvalidTokenError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="登录状态已失效，请重新登录。") from exc

    user = fetch_user_by_id(str(payload.get("sub", "")))
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="当前用户不存在，请重新登录。")
    return user


def read_store() -> dict[str, Any]:
    try:
        raw = json.loads(ARCHIVES_PATH.read_text(encoding="utf-8"))
    except FileNotFoundError:
        raw = {"updatedAt": iso_now(), "elders": []}

    elders = [normalize_archive(item) for item in raw.get("elders", [])]
    return {
        "updatedAt": raw.get("updatedAt") or iso_now(),
        "elders": elders,
    }


def write_store(store: dict[str, Any]) -> None:
    store["updatedAt"] = iso_now()
    normalized = {
        "updatedAt": store["updatedAt"],
        "elders": [normalize_archive(item) for item in store.get("elders", [])],
    }
    ARCHIVES_PATH.write_text(json.dumps(normalized, ensure_ascii=False, indent=2), encoding="utf-8")


def normalize_archive(archive: dict[str, Any]) -> dict[str, Any]:
    images = list(archive.get("assets", {}).get("images", [])) if isinstance(archive.get("assets"), dict) else []
    videos = list(archive.get("assets", {}).get("videos", [])) if isinstance(archive.get("assets"), dict) else []
    media = archive.get("media") if isinstance(archive.get("media"), dict) else {}
    created_at = archive.get("createdAt") or iso_now()
    updated_at = archive.get("updatedAt") or created_at

    return {
        "id": str(archive.get("id") or uuid.uuid4()),
        "name": str(archive.get("name") or "未命名档案").strip(),
        "age": int(archive.get("age") or 60),
        "hometown": str(archive.get("hometown") or "").strip(),
        "community": str(archive.get("community") or "").strip(),
        "role": str(archive.get("role") or "").strip(),
        "summary": str(archive.get("summary") or "").strip(),
        "wish": str(archive.get("wish") or "").strip(),
        "tags": [str(item).strip() for item in archive.get("tags", []) if str(item).strip()],
        "supporters": [str(item).strip() for item in archive.get("supporters", []) if str(item).strip()],
        "tone": str(archive.get("tone") or "amber"),
        "media": {
            "photos": max(int(media.get("photos", 0) or 0), len(images)),
            "audio": int(media.get("audio", 0) or 0),
            "video": max(int(media.get("video", 0) or 0), len(videos)),
            "documents": int(media.get("documents", 0) or 0),
        },
        "timeline": [
            {
                "year": str(item.get("year") or "").strip(),
                "title": str(item.get("title") or "").strip(),
                "description": str(item.get("description") or "").strip(),
            }
            for item in archive.get("timeline", [])
            if str(item.get("year") or "").strip() and str(item.get("title") or "").strip()
        ],
        "assets": {
            "images": [normalize_asset(item) for item in images],
            "videos": [normalize_asset(item) for item in videos],
        },
        "createdAt": created_at,
        "updatedAt": updated_at,
        "createdBy": str(archive.get("createdBy") or "").strip(),
    }


def normalize_asset(asset: dict[str, Any]) -> dict[str, Any]:
    return {
        "name": str(asset.get("name") or "未命名素材").strip(),
        "type": str(asset.get("type") or "application/octet-stream").strip(),
        "size": int(asset.get("size") or 0),
        "url": str(asset.get("url") or "").strip(),
        "uploadedAt": str(asset.get("uploadedAt") or iso_now()).strip(),
    }


def build_overview(elders: list[dict[str, Any]]) -> dict[str, int]:
    return {
        "totalArchives": len(elders),
        "activeCommunities": len({item["community"] for item in elders if item["community"]}),
        "totalTimelineEvents": sum(len(item["timeline"]) for item in elders),
        "totalMediaAssets": sum(
            item["media"]["photos"]
            + item["media"]["video"]
            + item["media"]["audio"]
            + item["media"]["documents"]
            for item in elders
        ),
        "totalSupportLinks": sum(len(item["supporters"]) for item in elders),
    }


def sanitize_filename(filename: str) -> str:
    stem = Path(filename).stem
    suffix = Path(filename).suffix
    safe_stem = re.sub(r"[^A-Za-z0-9\u4e00-\u9fff_-]+", "_", stem).strip("._-")[:40] or "asset"
    safe_suffix = re.sub(r"[^A-Za-z0-9.]+", "", suffix)[:12]
    return f"{safe_stem}{safe_suffix}"


def parse_data_url(data_url: str) -> tuple[str, bytes]:
    matched = re.fullmatch(r"data:([^;]+);base64,(.+)", data_url)
    if not matched:
        raise HTTPException(status_code=400, detail="上传素材格式无效。")

    mime_type = matched.group(1)
    try:
        content = base64.b64decode(matched.group(2), validate=True)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="上传素材内容无法解析。") from exc
    return mime_type, content


def save_asset_file(item: AssetPayload, archive_id: str, kind: str) -> dict[str, Any]:
    mime_type, content = parse_data_url(item.data_url)
    if not mime_type.startswith(f"{kind}/"):
        raise HTTPException(status_code=400, detail=f"上传的文件中包含非{ '图片' if kind == 'image' else '视频' }素材。")

    archive_dir = UPLOADS_DIR / archive_id
    archive_dir.mkdir(parents=True, exist_ok=True)

    extension = Path(item.name).suffix or mimetypes.guess_extension(mime_type) or ""
    filename = sanitize_filename(Path(item.name).stem + extension)
    final_name = f"{uuid.uuid4().hex[:10]}_{filename}"
    file_path = archive_dir / final_name
    file_path.write_bytes(content)

    return {
        "name": item.name,
        "type": mime_type,
        "size": len(content),
        "url": f"/uploads/{archive_id}/{final_name}",
        "uploadedAt": iso_now(),
    }


def save_assets(archive: dict[str, Any], images: list[AssetPayload], videos: list[AssetPayload]) -> None:
    saved_images = [save_asset_file(item, archive["id"], "image") for item in images]
    saved_videos = [save_asset_file(item, archive["id"], "video") for item in videos]
    archive["assets"]["images"].extend(saved_images)
    archive["assets"]["videos"].extend(saved_videos)
    archive["media"]["photos"] = max(archive["media"]["photos"], len(archive["assets"]["images"]))
    archive["media"]["video"] = max(archive["media"]["video"], len(archive["assets"]["videos"]))
    archive["updatedAt"] = iso_now()


def remove_archive_uploads(archive_id: str) -> None:
    shutil.rmtree(UPLOADS_DIR / archive_id, ignore_errors=True)


def remove_archive_index(archive_id: str) -> None:
    shutil.rmtree(RAG_DIR / archive_id, ignore_errors=True)


def build_archive_documents(archive: dict[str, Any]) -> list[Document]:
    base_documents = [
        Document(
            page_content=(
                f"姓名：{archive['name']}。年龄：{archive['age']}岁。籍贯：{archive['hometown']}。"
                f"社区：{archive['community']}。身份：{archive['role']}。"
            ),
            metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "人物信息"},
        ),
        Document(
            page_content=f"人物摘要：{archive['summary'] or '暂无摘要。'}",
            metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "故事摘要"},
        ),
        Document(
            page_content=f"当前愿望：{archive['wish'] or '暂无愿望记录。'}",
            metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "当前愿望"},
        ),
        Document(
            page_content=f"主题标签：{'、'.join(archive['tags']) or '暂无主题标签。'}",
            metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "主题标签"},
        ),
        Document(
            page_content=f"支持网络：{'、'.join(archive['supporters']) or '暂无支持网络记录。'}",
            metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "支持网络"},
        ),
    ]

    for item in archive["timeline"]:
        base_documents.append(
            Document(
                page_content=f"{item['year']}年：{item['title']}。{item['description']}",
                metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "生命时间线"},
            )
        )

    for item in archive["assets"]["images"]:
        base_documents.append(
            Document(
                page_content=f"图片素材：{item['name']}。上传时间：{item['uploadedAt']}。",
                metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "图片素材"},
            )
        )

    for item in archive["assets"]["videos"]:
        base_documents.append(
            Document(
                page_content=f"视频素材：{item['name']}。上传时间：{item['uploadedAt']}。",
                metadata={"archiveId": archive["id"], "archiveName": archive["name"], "section": "视频素材"},
            )
        )

    chunks: list[Document] = []
    for document in base_documents:
        for chunk in text_splitter.split_text(document.page_content):
            content = chunk.strip()
            if content:
                chunks.append(Document(page_content=content, metadata=document.metadata))
    return chunks


def rebuild_archive_index(archive: dict[str, Any]) -> int:
    documents = build_archive_documents(archive)
    index_dir = RAG_DIR / archive["id"]
    remove_archive_index(archive["id"])
    index_dir.mkdir(parents=True, exist_ok=True)

    if not documents:
        return 0

    vector_store = FAISS.from_documents(documents, embeddings)
    vector_store.save_local(str(index_dir))
    return len(documents)


def load_archive_index(archive: dict[str, Any]) -> FAISS:
    index_dir = RAG_DIR / archive["id"]
    if not index_dir.exists():
        rebuild_archive_index(archive)
    return FAISS.load_local(str(index_dir), embeddings, allow_dangerous_deserialization=True)


def generate_grounded_answer(archive: dict[str, Any], question: str, documents: list[Document]) -> str:
    if not documents:
        return f"我在 {archive['name']} 的档案里暂时没有检索到足够信息，建议先补充摘要、愿望或生命节点。"

    snippets: list[str] = []
    sections: list[str] = []
    for document in documents:
        snippet = re.sub(r"\s+", " ", document.page_content).strip()
        if snippet and snippet not in snippets:
            snippets.append(snippet[:120])
        section = str(document.metadata.get("section") or "").strip()
        if section and section not in sections:
            sections.append(section)

    joined = "；".join(snippets[:3])
    source_text = "、".join(sections[:3]) or "档案资料"

    if re.search(r"愿望|想|希望|打算", question):
        opening = f"如果按照档案里的记录来回答，{archive['name']}目前最明确的愿望相关信息是："
    elif re.search(r"家|亲人|支持|帮助|陪伴", question):
        opening = f"从支持网络和相关记录来看，围绕 {archive['name']} 的陪伴与协作主要体现在："
    elif re.search(r"经历|什么时候|哪年|时间线|过去", question):
        opening = f"根据生命时间线和档案片段，可以这样回顾 {archive['name']} 的经历："
    else:
        opening = f"基于当前检索到的 {source_text}，如果以 {archive['name']} 的数字档案来回应："

    return f"{opening}{joined}。如果你想继续追问，我可以继续基于这份档案补充回答。"


@app.on_event("startup")
def on_startup() -> None:
    init_storage()
    init_database()


@app.get("/api/health")
def health_check() -> dict[str, Any]:
    return {
        "status": "ok",
        "timestamp": iso_now(),
        "backend": "fastapi",
        "rag": "langchain + faiss",
    }


@app.post("/api/auth/register")
def register(payload: UserRegisterPayload) -> dict[str, Any]:
    existing = fetch_user_by_username(payload.username)
    if existing:
        raise HTTPException(status_code=409, detail="该用户名已注册。")

    user = {
        "id": str(uuid.uuid4()),
        "username": payload.username,
        "display_name": payload.display_name or payload.username,
        "password_hash": hash_password(payload.password),
        "created_at": iso_now(),
    }

    with get_db() as connection:
        connection.execute(
            """
            INSERT INTO users (id, username, display_name, password_hash, created_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            (
                user["id"],
                user["username"],
                user["display_name"],
                user["password_hash"],
                user["created_at"],
            ),
        )
        connection.commit()

    token = create_access_token(user)
    return {
        "accessToken": token,
        "user": {
            "id": user["id"],
            "username": user["username"],
            "displayName": user["display_name"],
        },
    }


@app.post("/api/auth/login")
def login(payload: UserLoginPayload) -> dict[str, Any]:
    user = fetch_user_by_username(payload.username)
    if not user or not verify_password(payload.password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="用户名或密码错误。")

    token = create_access_token(user)
    return {
        "accessToken": token,
        "user": {
            "id": user["id"],
            "username": user["username"],
            "displayName": user["display_name"],
        },
    }


@app.get("/api/auth/me")
def get_me(user: dict[str, Any] = Depends(ensure_authenticated)) -> dict[str, Any]:
    return {
        "user": {
            "id": user["id"],
            "username": user["username"],
            "displayName": user["display_name"],
            "createdAt": user["created_at"],
        }
    }


@app.get("/api/archives")
def get_archives(_: dict[str, Any] = Depends(ensure_authenticated)) -> dict[str, Any]:
    store = read_store()
    return {
        "elders": store["elders"],
        "overview": build_overview(store["elders"]),
        "updatedAt": store["updatedAt"],
    }


@app.get("/api/archives/{archive_id}")
def get_archive(archive_id: str, _: dict[str, Any] = Depends(ensure_authenticated)) -> dict[str, Any]:
    store = read_store()
    elder = next((item for item in store["elders"] if item["id"] == archive_id), None)
    if not elder:
        raise HTTPException(status_code=404, detail="未找到对应档案。")
    return {"elder": elder}


@app.post("/api/archives")
def create_archive(
    payload: ArchiveCreatePayload,
    user: dict[str, Any] = Depends(ensure_authenticated),
) -> dict[str, Any]:
    store = read_store()
    now = iso_now()
    archive = normalize_archive(
        {
            "id": str(uuid.uuid4()),
            "name": payload.name,
            "age": payload.age,
            "hometown": payload.hometown,
            "community": payload.community,
            "role": payload.role,
            "summary": payload.summary,
            "wish": payload.wish,
            "tags": payload.tags,
            "supporters": payload.supporters,
            "tone": payload.tone,
            "timeline": [],
            "assets": {"images": [], "videos": []},
            "createdAt": now,
            "updatedAt": now,
            "createdBy": user["username"],
        }
    )
    save_assets(archive, payload.images, payload.videos)
    store["elders"].insert(0, archive)
    write_store(store)
    rebuild_archive_index(archive)
    return {"elder": archive}


@app.post("/api/archives/{archive_id}/timeline")
def append_timeline(
    archive_id: str,
    payload: TimelinePayload,
    _: dict[str, Any] = Depends(ensure_authenticated),
) -> dict[str, Any]:
    store = read_store()
    elder = next((item for item in store["elders"] if item["id"] == archive_id), None)
    if not elder:
        raise HTTPException(status_code=404, detail="未找到对应档案。")

    if not payload.year or not payload.title:
        raise HTTPException(status_code=400, detail="年份和标题不能为空。")

    elder["timeline"].append(
        {
            "year": payload.year[:12],
            "title": payload.title[:40],
            "description": payload.description[:160],
        }
    )
    elder["updatedAt"] = iso_now()
    write_store(store)
    rebuild_archive_index(elder)
    return {"elder": elder}


@app.post("/api/archives/{archive_id}/assets")
def append_assets(
    archive_id: str,
    payload: AssetUploadRequest,
    _: dict[str, Any] = Depends(ensure_authenticated),
) -> dict[str, Any]:
    if not payload.images and not payload.videos:
        raise HTTPException(status_code=400, detail="请至少上传一张图片或一个视频。")

    store = read_store()
    elder = next((item for item in store["elders"] if item["id"] == archive_id), None)
    if not elder:
        raise HTTPException(status_code=404, detail="未找到对应档案。")

    save_assets(elder, payload.images, payload.videos)
    write_store(store)
    rebuild_archive_index(elder)
    return {"elder": elder}


@app.delete("/api/archives/{archive_id}")
def delete_archive(archive_id: str, _: dict[str, Any] = Depends(ensure_authenticated)) -> dict[str, Any]:
    store = read_store()
    elder = next((item for item in store["elders"] if item["id"] == archive_id), None)
    if not elder:
        raise HTTPException(status_code=404, detail="未找到对应档案。")

    store["elders"] = [item for item in store["elders"] if item["id"] != archive_id]
    write_store(store)
    remove_archive_uploads(archive_id)
    remove_archive_index(archive_id)
    return {"success": True}


@app.post("/api/rag/rebuild")
def rebuild_rag(
    payload: RagRebuildRequest,
    _: dict[str, Any] = Depends(ensure_authenticated),
) -> dict[str, Any]:
    store = read_store()
    if payload.archive_id:
        archive = next((item for item in store["elders"] if item["id"] == payload.archive_id), None)
        if not archive:
            raise HTTPException(status_code=404, detail="未找到要重建的档案。")
        chunks = rebuild_archive_index(archive)
        return {"rebuilt": 1, "chunks": chunks}

    total_chunks = 0
    for archive in store["elders"]:
        total_chunks += rebuild_archive_index(archive)
    return {"rebuilt": len(store["elders"]), "chunks": total_chunks}


@app.post("/api/rag/ask")
def ask_rag(
    payload: RagAskRequest,
    _: dict[str, Any] = Depends(ensure_authenticated),
) -> dict[str, Any]:
    store = read_store()
    archive = next((item for item in store["elders"] if item["id"] == payload.archive_id), None)
    if not archive:
        raise HTTPException(status_code=404, detail="未找到对应档案。")

    vector_store = load_archive_index(archive)
    results = vector_store.similarity_search_with_score(payload.question, k=4)
    documents = [item[0] for item in results]
    answer = generate_grounded_answer(archive, payload.question, documents)

    sources = []
    for document, score in results:
        sources.append(
            {
                "section": document.metadata.get("section", "档案片段"),
                "preview": re.sub(r"\s+", " ", document.page_content).strip()[:160],
                "score": round(float(score), 4),
            }
        )

    return {
        "answer": answer,
        "archive": {"id": archive["id"], "name": archive["name"]},
        "sources": sources,
        "workflow": ["档案文本", "embedding", "FAISS 向量检索", "数字生命回答"],
    }


@app.get("/")
def serve_index() -> FileResponse:
    return FileResponse(BASE_DIR / "index.html")


@app.get("/{file_name}")
def serve_static_file(file_name: str) -> FileResponse:
    if file_name not in STATIC_FILES:
        raise HTTPException(status_code=404, detail="文件不存在。")
    file_path = BASE_DIR / file_name
    if not file_path.exists():
        raise HTTPException(status_code=404, detail="文件不存在。")
    return FileResponse(file_path)
