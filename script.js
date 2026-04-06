const FILE_LIMITS = {
  image: 8 * 1024 * 1024,
  video: 35 * 1024 * 1024,
  total: 60 * 1024 * 1024
};

const STORAGE_KEYS = {
  token: "elder-life-archive-token"
};

const toneLabels = {
  amber: "暖铜晨光",
  jade: "松石纸本",
  rose: "晚霞留影"
};

const state = {
  token: window.localStorage.getItem(STORAGE_KEYS.token) || "",
  currentUser: null,
  archives: [],
  overview: null,
  query: "",
  activeTag: "全部",
  sortBy: "recent",
  selectedId: null,
  authMode: "login",
  rag: createEmptyRagState()
};

const elements = {
  authStage: document.getElementById("auth-stage"),
  appShell: document.getElementById("app-shell"),
  loginTab: document.getElementById("login-tab"),
  registerTab: document.getElementById("register-tab"),
  loginForm: document.getElementById("login-form"),
  registerForm: document.getElementById("register-form"),
  userSession: document.getElementById("user-session"),
  userDisplay: document.getElementById("user-display"),
  logoutBtn: document.getElementById("logout-btn"),
  refreshBtn: document.getElementById("refresh-btn"),
  viewSampleBtn: document.getElementById("view-sample-btn"),
  metrics: document.getElementById("metrics"),
  resultMeta: document.getElementById("result-meta"),
  archiveGrid: document.getElementById("archive-grid"),
  tagStrip: document.getElementById("tag-strip"),
  searchInput: document.getElementById("search-input"),
  sortSelect: document.getElementById("sort-select"),
  createForm: document.getElementById("create-form"),
  createImagesInput: document.getElementById("create-images"),
  createVideosInput: document.getElementById("create-videos"),
  createImagesHint: document.getElementById("create-images-hint"),
  createVideosHint: document.getElementById("create-videos-hint"),
  detailDrawer: document.getElementById("detail-drawer"),
  detailContent: document.getElementById("detail-content"),
  drawerBackdrop: document.getElementById("drawer-backdrop"),
  closeDrawerBtn: document.getElementById("close-drawer-btn"),
  toast: document.getElementById("toast")
};

document.addEventListener("DOMContentLoaded", () => {
  bindEvents();
  bootstrap();
});

function bindEvents() {
  document.querySelectorAll("[data-auth-mode]").forEach((button) => {
    button.addEventListener("click", () => {
      setAuthMode(button.dataset.authMode || "login");
    });
  });

  elements.loginForm.addEventListener("submit", handleLoginSubmit);
  elements.registerForm.addEventListener("submit", handleRegisterSubmit);
  elements.logoutBtn.addEventListener("click", handleLogout);
  elements.refreshBtn.addEventListener("click", () => {
    loadArchives(state.selectedId);
  });

  elements.viewSampleBtn.addEventListener("click", () => {
    const firstArchive = getFilteredArchives()[0] || state.archives[0];
    if (!firstArchive) {
      showToast("当前还没有可查看的档案。", true);
      return;
    }
    loadArchiveDetail(firstArchive.id, true);
  });

  elements.searchInput.addEventListener("input", (event) => {
    state.query = event.target.value.trim();
    renderArchiveGrid();
  });

  elements.sortSelect.addEventListener("change", (event) => {
    state.sortBy = event.target.value;
    renderArchiveGrid();
  });

  elements.tagStrip.addEventListener("click", (event) => {
    const button = event.target.closest("[data-tag]");
    if (!button) {
      return;
    }

    state.activeTag = button.dataset.tag || "全部";
    renderTagStrip();
    renderArchiveGrid();
  });

  elements.archiveGrid.addEventListener("click", (event) => {
    const card = event.target.closest("[data-archive-id]");
    if (!card) {
      return;
    }
    loadArchiveDetail(card.dataset.archiveId, true);
  });

  elements.createImagesInput.addEventListener("change", () => {
    updateFileHint(elements.createImagesInput, elements.createImagesHint, "图片", FILE_LIMITS.image);
  });

  elements.createVideosInput.addEventListener("change", () => {
    updateFileHint(elements.createVideosInput, elements.createVideosHint, "视频", FILE_LIMITS.video);
  });

  elements.createForm.addEventListener("submit", handleCreateArchiveSubmit);
  elements.drawerBackdrop.addEventListener("click", closeDrawer);
  elements.closeDrawerBtn.addEventListener("click", closeDrawer);

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeDrawer();
    }
  });
}

async function bootstrap() {
  setAuthMode("login");

  if (!state.token) {
    showAuthStage();
    return;
  }

  try {
    const data = await apiRequest("/api/auth/me", { skipAuthRedirect: true });
    applySession({ accessToken: state.token, user: data.user });
    await loadArchives();
  } catch (error) {
    clearSession();
    showToast(error.message || "登录状态已失效，请重新登录。", true);
  }
}

function setAuthMode(mode) {
  state.authMode = mode === "register" ? "register" : "login";
  const isLogin = state.authMode === "login";
  elements.loginTab.classList.toggle("active", isLogin);
  elements.registerTab.classList.toggle("active", !isLogin);
  elements.loginForm.classList.toggle("is-hidden", !isLogin);
  elements.registerForm.classList.toggle("is-hidden", isLogin);
}

async function handleLoginSubmit(event) {
  event.preventDefault();
  const submitButton = elements.loginForm.querySelector('button[type="submit"]');
  const formData = new FormData(elements.loginForm);

  submitButton.disabled = true;
  submitButton.textContent = "正在登录...";

  try {
    const data = await apiRequest("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({
        username: String(formData.get("username") || "").trim(),
        password: String(formData.get("password") || "")
      }),
      skipAuthRedirect: true
    });

    elements.loginForm.reset();
    applySession(data);
    await loadArchives();
    showToast("登录成功，已进入档案工作台。");
  } catch (error) {
    showToast(error.message, true);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "立即登录";
  }
}

async function handleRegisterSubmit(event) {
  event.preventDefault();
  const submitButton = elements.registerForm.querySelector('button[type="submit"]');
  const formData = new FormData(elements.registerForm);
  const password = String(formData.get("password") || "");
  const confirmPassword = String(formData.get("confirmPassword") || "");

  if (password !== confirmPassword) {
    showToast("两次输入的密码不一致。", true);
    return;
  }

  submitButton.disabled = true;
  submitButton.textContent = "正在注册...";

  try {
    const data = await apiRequest("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({
        displayName: String(formData.get("displayName") || "").trim(),
        username: String(formData.get("username") || "").trim(),
        password
      }),
      skipAuthRedirect: true
    });

    elements.registerForm.reset();
    applySession(data);
    await loadArchives();
    showToast("注册成功，已自动登录。");
  } catch (error) {
    showToast(error.message, true);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "注册并进入系统";
  }
}

function handleLogout() {
  clearSession();
  showToast("你已退出登录。");
}

function applySession(payload) {
  state.token = payload.accessToken || state.token;
  state.currentUser = payload.user || null;
  if (state.token) {
    window.localStorage.setItem(STORAGE_KEYS.token, state.token);
  }
  elements.userDisplay.textContent = `当前用户：${state.currentUser?.displayName || state.currentUser?.username || "未登录"}`;
  showAppStage();
}

function clearSession() {
  state.token = "";
  state.currentUser = null;
  state.archives = [];
  state.overview = null;
  state.query = "";
  state.activeTag = "全部";
  state.sortBy = "recent";
  state.selectedId = null;
  state.rag = createEmptyRagState();
  window.localStorage.removeItem(STORAGE_KEYS.token);
  elements.loginForm.reset();
  elements.registerForm.reset();
  elements.searchInput.value = "";
  elements.sortSelect.value = "recent";
  renderMetrics();
  renderTagStrip();
  renderArchiveGrid();
  closeDrawer();
  showAuthStage();
}

function showAuthStage() {
  elements.authStage.classList.remove("is-hidden");
  elements.appShell.classList.add("is-hidden");
  elements.userSession.classList.add("is-hidden");
  elements.refreshBtn.classList.add("is-hidden");
  elements.resultMeta.textContent = "请先登录";
}

function showAppStage() {
  elements.authStage.classList.add("is-hidden");
  elements.appShell.classList.remove("is-hidden");
  elements.userSession.classList.remove("is-hidden");
  elements.refreshBtn.classList.remove("is-hidden");
}

async function loadArchives(focusId) {
  try {
    const data = await apiRequest("/api/archives");
    state.archives = data.elders.map(normalizeArchive);
    state.overview = data.overview;

    if (!state.archives.some((archive) => archive.id === state.selectedId)) {
      state.selectedId = null;
    }
    if (focusId && state.archives.some((archive) => archive.id === focusId)) {
      state.selectedId = focusId;
    }

    renderMetrics();
    renderTagStrip();
    renderArchiveGrid();
  } catch (error) {
    elements.resultMeta.textContent = "档案加载失败";
    elements.archiveGrid.innerHTML = `
      <article class="empty-state panel">
        <p class="section-kicker">接口异常</p>
        <h3>暂时无法读取档案数据</h3>
        <p>${escapeHtml(error.message)}</p>
      </article>
    `;
    showToast(error.message, true);
  }
}

async function loadArchiveDetail(archiveId, openAfterLoad) {
  try {
    const data = await apiRequest(`/api/archives/${archiveId}`);
    const archive = normalizeArchive(data.elder);
    replaceArchive(archive);
    state.selectedId = archive.id;

    if (state.rag.archiveId !== archive.id) {
      state.rag = createEmptyRagState(archive.id);
    }

    renderArchiveGrid();
    renderDetail(archive);

    if (openAfterLoad) {
      openDrawer();
    }
  } catch (error) {
    showToast(error.message, true);
  }
}

function replaceArchive(archive) {
  const index = state.archives.findIndex((item) => item.id === archive.id);
  if (index === -1) {
    state.archives.unshift(archive);
    return;
  }
  state.archives.splice(index, 1, archive);
}

function renderMetrics() {
  const overview = state.overview || {
    totalArchives: 0,
    activeCommunities: 0,
    totalTimelineEvents: 0,
    totalMediaAssets: 0,
    totalSupportLinks: 0
  };

  const metrics = [
    {
      label: "建档长者",
      value: overview.totalArchives,
      helper: `${overview.activeCommunities} 个社区被点亮`
    },
    {
      label: "生命节点",
      value: overview.totalTimelineEvents,
      helper: "记录个人轨迹与时代切片"
    },
    {
      label: "多模态素材",
      value: overview.totalMediaAssets,
      helper: "文字、图片、视频共同构成"
    },
    {
      label: "支持网络",
      value: overview.totalSupportLinks,
      helper: "家属、社工与志愿者协作"
    }
  ];

  elements.metrics.innerHTML = metrics
    .map((metric, index) => `
      <article class="metric-card panel" style="--delay:${index * 80}ms">
        <p>${metric.label}</p>
        <strong>${metric.value}</strong>
        <span>${metric.helper}</span>
      </article>
    `)
    .join("");
}

function renderTagStrip() {
  const tags = new Set(["全部"]);
  state.archives.forEach((archive) => {
    archive.tags.forEach((tag) => tags.add(tag));
  });

  if (!tags.has(state.activeTag)) {
    state.activeTag = "全部";
  }

  elements.tagStrip.innerHTML = Array.from(tags)
    .map((tag) => `
      <button
        class="tag-chip ${tag === state.activeTag ? "active" : ""}"
        type="button"
        data-tag="${escapeAttribute(tag)}"
      >
        ${escapeHtml(tag)}
      </button>
    `)
    .join("");
}

function renderArchiveGrid() {
  const archives = getFilteredArchives();
  const userLabel = state.currentUser ? ` · ${state.currentUser.displayName || state.currentUser.username}` : "";
  elements.resultMeta.textContent = `共 ${archives.length} 份档案${userLabel}`;

  if (!archives.length) {
    elements.archiveGrid.innerHTML = `
      <article class="empty-state panel">
        <p class="section-kicker">暂无匹配结果</p>
        <h3>换一个关键词或标签试试</h3>
        <p>你也可以直接在右侧创建一份新的生命叙事档案。</p>
      </article>
    `;
    return;
  }

  elements.archiveGrid.innerHTML = archives
    .map((archive, index) => {
      const selectedClass = archive.id === state.selectedId ? "is-selected" : "";
      const importedMedia = archive.assets.images.length + archive.assets.videos.length;

      return `
        <article
          class="archive-card ${selectedClass}"
          data-tone="${escapeAttribute(archive.tone)}"
          data-archive-id="${escapeAttribute(archive.id)}"
          style="--delay:${index * 70}ms"
        >
          <div class="archive-card-media">
            ${renderCardVisual(archive)}
            <div class="archive-card-overlay">
              <span>查看档案</span>
            </div>
          </div>

          <div class="archive-card-body">
            <div class="archive-card-top">
              <span class="archive-badge">${escapeHtml(archive.role || "人物档案")}</span>
              <span class="archive-tone">${escapeHtml(toneLabels[archive.tone] || "档案色调")}</span>
            </div>

            <h3>${escapeHtml(archive.name)}</h3>
            <p class="archive-meta">${archive.age} 岁 · ${escapeHtml(archive.hometown || "籍贯待补充")} · ${escapeHtml(archive.community || "社区待补充")}</p>
            <p class="archive-summary">${escapeHtml(archive.summary || "这份档案还没有补充故事摘要。")}</p>

            <div class="archive-tags">
              ${archive.tags.slice(0, 3).map((tag) => `<span>${escapeHtml(tag)}</span>`).join("") || "<span>待补充标签</span>"}
            </div>

            <div class="archive-footer">
              <span>${archive.timeline.length} 个生命节点</span>
              <span>${archive.media.photos} 张图 / ${archive.media.video} 段视频</span>
            </div>

            <div class="archive-footnote">
              <span>${importedMedia} 个已导入素材可直接预览</span>
            </div>
          </div>
        </article>
      `;
    })
    .join("");
}

function renderDetail(archive) {
  const timelineItems = archive.timeline.length
    ? archive.timeline.map((item) => `
        <li>
          <div class="timeline-year">${escapeHtml(item.year)}</div>
          <div class="timeline-copy">
            <h4>${escapeHtml(item.title)}</h4>
            <p>${escapeHtml(item.description || "暂无补充描述。")}</p>
          </div>
        </li>
      `).join("")
    : `
      <li class="timeline-empty">
        <div class="timeline-copy">
          <h4>还没有生命节点</h4>
          <p>可以在下方补充一段值得被记住的经历。</p>
        </div>
      </li>
    `;

  const supporters = archive.supporters.length
    ? archive.supporters.map((item) => `<span>${escapeHtml(item)}</span>`).join("")
    : "<span>待补充支持网络</span>";

  const tags = archive.tags.length
    ? archive.tags.map((item) => `<span>${escapeHtml(item)}</span>`).join("")
    : "<span>待补充主题标签</span>";

  const imageGallery = archive.assets.images.length
    ? archive.assets.images.map((asset) => `
        <a class="media-card media-card-image" href="${escapeAttribute(asset.url)}" target="_blank" rel="noreferrer">
          <img src="${escapeAttribute(asset.url)}" alt="${escapeAttribute(asset.name || archive.name)}" loading="lazy">
          <span>${escapeHtml(asset.name || "图片素材")}</span>
        </a>
      `).join("")
    : '<div class="media-empty">还没有导入图片素材。</div>';

  const videoGallery = archive.assets.videos.length
    ? archive.assets.videos.map((asset) => `
        <article class="media-card media-card-video">
          <video controls preload="metadata" src="${escapeAttribute(asset.url)}"></video>
          <span>${escapeHtml(asset.name || "视频素材")}</span>
        </article>
      `).join("")
    : '<div class="media-empty">还没有导入视频素材。</div>';

  elements.detailContent.innerHTML = `
    <section class="detail-hero" data-tone="${escapeAttribute(archive.tone)}">
      <div class="detail-hero-main">
        <div>
          <p class="section-kicker">人物详情</p>
          <h3>${escapeHtml(archive.name)}</h3>
          <p class="detail-subtitle">${archive.age} 岁 · ${escapeHtml(archive.role || "人物档案")} · ${escapeHtml(archive.community || "社区待补充")}</p>
          <p class="detail-summary">${escapeHtml(archive.summary || "这份档案还没有补充故事摘要。")}</p>
        </div>
        <div class="detail-hero-visual">
          ${renderDetailVisual(archive)}
        </div>
      </div>
    </section>

    <section class="detail-grid">
      <article class="detail-panel">
        <p class="detail-label">当前愿望</p>
        <p>${escapeHtml(archive.wish || "暂未记录当前愿望。")}</p>
      </article>

      <article class="detail-panel">
        <p class="detail-label">多模态统计</p>
        <div class="media-metrics">
          <span>图片 ${archive.media.photos}</span>
          <span>视频 ${archive.media.video}</span>
          <span>音频 ${archive.media.audio}</span>
          <span>文稿 ${archive.media.documents}</span>
        </div>
      </article>
    </section>

    <section class="detail-panel">
      <div class="detail-headline">
        <div>
          <p class="detail-label">主题标签</p>
          <h4>故事关键词</h4>
        </div>
      </div>
      <div class="detail-tags">${tags}</div>
    </section>

    <section class="detail-panel dialogue-panel">
      <div class="detail-headline">
        <div>
          <p class="detail-label">数字生命对话</p>
          <h4>基于档案内容进行智能问答</h4>
        </div>
        <button class="secondary-button" id="rebuild-rag-btn" type="button">重建知识库</button>
      </div>

      <form id="rag-form" class="stack-form compact-form">
        <label>
          <span>请输入问题</span>
          <textarea
            id="rag-question"
            name="question"
            rows="3"
            maxlength="180"
            placeholder="例如：她现在最想完成的事情是什么？她的生活中有哪些重要支持者？"
            required
          >${escapeHtml(state.rag.archiveId === archive.id ? state.rag.lastQuestion : "")}</textarea>
        </label>

        <div class="detail-actions">
          <button class="primary-button" id="rag-submit-btn" type="submit">开始提问</button>
        </div>
      </form>

      <div class="rag-answer-box" id="rag-answer-box">
        ${renderRagAnswer(archive)}
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-headline">
        <div>
          <p class="detail-label">素材图库</p>
          <h4>已导入的图片与视频</h4>
        </div>
      </div>

      <div class="detail-media-section">
        <div>
          <p class="detail-label">图片</p>
          <div class="media-gallery media-gallery-images">${imageGallery}</div>
        </div>

        <div>
          <p class="detail-label">视频</p>
          <div class="media-gallery media-gallery-videos">${videoGallery}</div>
        </div>
      </div>
    </section>

    <section class="detail-panel">
      <div class="detail-headline">
        <div>
          <p class="detail-label">导入素材</p>
          <h4>继续补充图片和视频</h4>
        </div>
      </div>

      <form id="asset-form" class="stack-form compact-form">
        <div class="upload-stack">
          <label class="upload-field">
            <span>新增图片</span>
            <input id="detail-images" type="file" accept="image/*" multiple>
            <small id="detail-images-hint">支持多张图片一起上传。</small>
          </label>

          <label class="upload-field">
            <span>新增视频</span>
            <input id="detail-videos" type="file" accept="video/*" multiple>
            <small id="detail-videos-hint">建议上传压缩后的短视频，速度更稳定。</small>
          </label>
        </div>

        <div class="detail-actions">
          <button class="primary-button" id="asset-submit-btn" type="submit">上传素材</button>
        </div>
      </form>
    </section>

    <section class="detail-panel">
      <div class="detail-headline">
        <div>
          <p class="detail-label">生命时间线</p>
          <h4>关键经历</h4>
        </div>
      </div>
      <ol class="timeline-list">${timelineItems}</ol>
    </section>

    <section class="detail-panel">
      <div class="detail-headline">
        <div>
          <p class="detail-label">支持网络</p>
          <h4>陪伴与协作</h4>
        </div>
      </div>
      <div class="detail-tags">${supporters}</div>
    </section>

    <section class="detail-panel">
      <div class="detail-headline">
        <div>
          <p class="detail-label">补充节点</p>
          <h4>为这份档案再添一笔</h4>
        </div>
      </div>

      <form id="timeline-form" class="stack-form compact-form">
        <div class="field-row">
          <label>
            <span>年份</span>
            <input name="year" type="text" maxlength="12" placeholder="例如：1998" required>
          </label>
          <label>
            <span>标题</span>
            <input name="title" type="text" maxlength="40" placeholder="例如：第一次学会视频通话" required>
          </label>
        </div>

        <label>
          <span>描述</span>
          <textarea name="description" rows="3" maxlength="120" placeholder="用一两句话补充当时发生了什么"></textarea>
        </label>

        <div class="detail-actions">
          <button class="primary-button" type="submit">保存节点</button>
          <button class="danger-button" id="delete-archive-btn" type="button">删除档案</button>
        </div>
      </form>
    </section>
  `;

  bindDetailEvents(archive);
}

function bindDetailEvents(archive) {
  const timelineForm = document.getElementById("timeline-form");
  const deleteButton = document.getElementById("delete-archive-btn");
  const assetForm = document.getElementById("asset-form");
  const detailImagesInput = document.getElementById("detail-images");
  const detailVideosInput = document.getElementById("detail-videos");
  const detailImagesHint = document.getElementById("detail-images-hint");
  const detailVideosHint = document.getElementById("detail-videos-hint");
  const assetSubmitButton = document.getElementById("asset-submit-btn");
  const ragForm = document.getElementById("rag-form");
  const ragSubmitButton = document.getElementById("rag-submit-btn");
  const rebuildRagButton = document.getElementById("rebuild-rag-btn");

  detailImagesInput.addEventListener("change", () => {
    updateFileHint(detailImagesInput, detailImagesHint, "图片", FILE_LIMITS.image);
  });

  detailVideosInput.addEventListener("change", () => {
    updateFileHint(detailVideosInput, detailVideosHint, "视频", FILE_LIMITS.video);
  });

  assetForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    assetSubmitButton.disabled = true;
    assetSubmitButton.textContent = "正在上传...";

    try {
      const images = await serializeFiles(detailImagesInput.files, "image");
      const videos = await serializeFiles(detailVideosInput.files, "video");

      if (!images.length && !videos.length) {
        throw new Error("请至少选择一张图片或一个视频。");
      }

      await apiRequest(`/api/archives/${archive.id}/assets`, {
        method: "POST",
        body: JSON.stringify({ images, videos })
      });

      showToast("素材导入完成。");
      await loadArchives(archive.id);
      await loadArchiveDetail(archive.id, true);
    } catch (error) {
      showToast(error.message, true);
    } finally {
      assetSubmitButton.disabled = false;
      assetSubmitButton.textContent = "上传素材";
    }
  });

  timelineForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(timelineForm);

    try {
      await apiRequest(`/api/archives/${archive.id}/timeline`, {
        method: "POST",
        body: JSON.stringify({
          year: String(formData.get("year") || "").trim(),
          title: String(formData.get("title") || "").trim(),
          description: String(formData.get("description") || "").trim()
        })
      });

      showToast("生命节点已补充。");
      await loadArchives(archive.id);
      await loadArchiveDetail(archive.id, true);
    } catch (error) {
      showToast(error.message, true);
    }
  });

  deleteButton.addEventListener("click", async () => {
    const confirmed = window.confirm(`确定删除 ${archive.name} 的档案吗？已上传的本地素材也会一并删除。`);
    if (!confirmed) {
      return;
    }

    try {
      await apiRequest(`/api/archives/${archive.id}`, { method: "DELETE" });
      state.selectedId = null;
      state.rag = createEmptyRagState();
      closeDrawer();
      showToast("档案已删除。");
      await loadArchives();
    } catch (error) {
      showToast(error.message, true);
    }
  });

  ragForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const question = String(new FormData(ragForm).get("question") || "").trim();

    if (!question) {
      showToast("请输入问题。", true);
      return;
    }

    ragSubmitButton.disabled = true;
    ragSubmitButton.textContent = "正在检索...";

    try {
      const data = await apiRequest("/api/rag/ask", {
        method: "POST",
        body: JSON.stringify({
          archiveId: archive.id,
          question
        })
      });

      state.rag = {
        archiveId: archive.id,
        lastQuestion: question,
        answer: data.answer || "",
        sources: Array.isArray(data.sources) ? data.sources : [],
        workflow: Array.isArray(data.workflow) ? data.workflow : []
      };
      updateRagAnswerView(archive);
      showToast("数字生命回答已生成。");
    } catch (error) {
      showToast(error.message, true);
    } finally {
      ragSubmitButton.disabled = false;
      ragSubmitButton.textContent = "开始提问";
    }
  });

  rebuildRagButton.addEventListener("click", async () => {
    rebuildRagButton.disabled = true;
    rebuildRagButton.textContent = "重建中...";

    try {
      await apiRequest("/api/rag/rebuild", {
        method: "POST",
        body: JSON.stringify({ archiveId: archive.id })
      });
      showToast("知识库已重建。");
    } catch (error) {
      showToast(error.message, true);
    } finally {
      rebuildRagButton.disabled = false;
      rebuildRagButton.textContent = "重建知识库";
    }
  });
}

function updateRagAnswerView(archive) {
  const answerBox = document.getElementById("rag-answer-box");
  if (!answerBox) {
    return;
  }
  answerBox.innerHTML = renderRagAnswer(archive);
}

function renderRagAnswer(archive) {
  if (state.rag.archiveId !== archive.id || !state.rag.answer) {
    return `
      <div class="rag-answer-empty">
        <p class="detail-label">对话说明</p>
        <h4>这里会显示基于档案内容生成的回答</h4>
        <p>你可以询问愿望、重要经历、支持网络、素材线索等问题，系统会先检索档案，再生成中文回答。</p>
      </div>
    `;
  }

  const sourceMarkup = state.rag.sources.length
    ? state.rag.sources.map((source) => `
        <article class="rag-source-card">
          <p class="rag-source-head">${escapeHtml(source.section || "档案片段")}</p>
          <p>${escapeHtml(source.preview || "")}</p>
        </article>
      `).join("")
    : '<div class="media-empty">当前没有返回可展示的检索来源。</div>';

  const workflow = state.rag.workflow.length
    ? state.rag.workflow.map((item) => `<span>${escapeHtml(item)}</span>`).join("")
    : "<span>档案文本</span><span>embedding</span><span>向量检索</span><span>生成回答</span>";

  return `
    <div class="rag-answer-result">
      <p class="detail-label">问题</p>
      <p class="rag-answer-question">${escapeHtml(state.rag.lastQuestion)}</p>
      <p class="detail-label">回答</p>
      <p class="rag-answer-text">${escapeHtml(state.rag.answer)}</p>
      <div class="rag-workflow-inline">${workflow}</div>
      <div class="rag-source-list">
        ${sourceMarkup}
      </div>
    </div>
  `;
}

async function handleCreateArchiveSubmit(event) {
  event.preventDefault();
  const submitButton = elements.createForm.querySelector('button[type="submit"]');

  submitButton.disabled = true;
  submitButton.textContent = "正在创建...";

  try {
    const formData = new FormData(elements.createForm);
    const images = await serializeFiles(elements.createImagesInput.files, "image");
    const videos = await serializeFiles(elements.createVideosInput.files, "video");

    const data = await apiRequest("/api/archives", {
      method: "POST",
      body: JSON.stringify({
        name: String(formData.get("name") || "").trim(),
        age: Number(formData.get("age") || 0),
        hometown: String(formData.get("hometown") || "").trim(),
        community: String(formData.get("community") || "").trim(),
        role: String(formData.get("role") || "").trim(),
        summary: String(formData.get("summary") || "").trim(),
        wish: String(formData.get("wish") || "").trim(),
        tags: splitListInput(formData.get("tags")),
        supporters: splitListInput(formData.get("supporters")),
        tone: String(formData.get("tone") || "amber"),
        images,
        videos
      })
    });

    elements.createForm.reset();
    elements.createForm.elements.tone.value = "amber";
    updateFileHint(elements.createImagesInput, elements.createImagesHint, "图片", FILE_LIMITS.image);
    updateFileHint(elements.createVideosInput, elements.createVideosHint, "视频", FILE_LIMITS.video);
    showToast(`已创建 ${data.elder.name} 的档案。`);
    await loadArchives(data.elder.id);
    await loadArchiveDetail(data.elder.id, true);
  } catch (error) {
    showToast(error.message, true);
  } finally {
    submitButton.disabled = false;
    submitButton.textContent = "创建档案";
  }
}

function getFilteredArchives() {
  const query = state.query.toLowerCase();

  return state.archives
    .filter((archive) => {
      const matchesTag = state.activeTag === "全部" || archive.tags.includes(state.activeTag);
      const searchText = [
        archive.name,
        archive.hometown,
        archive.community,
        archive.role,
        archive.summary,
        archive.wish,
        archive.tags.join(" "),
        archive.supporters.join(" "),
        archive.assets.images.map((asset) => asset.name).join(" "),
        archive.assets.videos.map((asset) => asset.name).join(" ")
      ].join(" ").toLowerCase();
      const matchesQuery = !query || searchText.includes(query);
      return matchesTag && matchesQuery;
    })
    .sort((left, right) => {
      if (state.sortBy === "age-desc") {
        return right.age - left.age;
      }
      if (state.sortBy === "timeline-desc") {
        return right.timeline.length - left.timeline.length;
      }
      return Date.parse(right.updatedAt) - Date.parse(left.updatedAt);
    });
}

function normalizeArchive(archive) {
  return {
    ...archive,
    tags: Array.isArray(archive.tags) ? archive.tags : [],
    supporters: Array.isArray(archive.supporters) ? archive.supporters : [],
    timeline: Array.isArray(archive.timeline) ? archive.timeline : [],
    media: {
      photos: Number(archive.media?.photos || 0),
      audio: Number(archive.media?.audio || 0),
      video: Number(archive.media?.video || 0),
      documents: Number(archive.media?.documents || 0)
    },
    assets: {
      images: Array.isArray(archive.assets?.images) ? archive.assets.images : [],
      videos: Array.isArray(archive.assets?.videos) ? archive.assets.videos : []
    }
  };
}

function renderCardVisual(archive) {
  const firstImage = archive.assets.images[0];
  if (firstImage) {
    return `
      <img
        class="archive-visual-image"
        src="${escapeAttribute(firstImage.url)}"
        alt="${escapeAttribute(archive.name)}"
        loading="lazy"
      >
    `;
  }

  const firstVideo = archive.assets.videos[0];
  if (firstVideo) {
    return `
      <div class="archive-visual-video">
        <video class="archive-visual-video-el" src="${escapeAttribute(firstVideo.url)}" muted playsinline preload="metadata"></video>
        <span>视频档案</span>
      </div>
    `;
  }

  return `
    <div class="archive-visual-fallback">
      <strong>${escapeHtml(getNameBadge(archive.name))}</strong>
      <span>${escapeHtml(archive.community || "未补充社区")}</span>
    </div>
  `;
}

function renderDetailVisual(archive) {
  const firstImage = archive.assets.images[0];
  if (firstImage) {
    return `<img src="${escapeAttribute(firstImage.url)}" alt="${escapeAttribute(archive.name)}" loading="lazy">`;
  }

  const firstVideo = archive.assets.videos[0];
  if (firstVideo) {
    return `<video controls preload="metadata" src="${escapeAttribute(firstVideo.url)}"></video>`;
  }

  return `
    <div class="detail-visual-fallback">
      <strong>${escapeHtml(getNameBadge(archive.name))}</strong>
      <span>${escapeHtml(archive.role || "人物档案")}</span>
    </div>
  `;
}

function getNameBadge(name) {
  return Array.from(String(name || "").trim()).slice(0, 2).join("") || "档案";
}

function openDrawer() {
  elements.detailDrawer.classList.add("open");
  elements.detailDrawer.setAttribute("aria-hidden", "false");
  elements.drawerBackdrop.hidden = false;
}

function closeDrawer() {
  elements.detailDrawer.classList.remove("open");
  elements.detailDrawer.setAttribute("aria-hidden", "true");
  elements.drawerBackdrop.hidden = true;
}

async function apiRequest(url, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (state.token && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${state.token}`);
  }

  const response = await fetch(url, {
    ...options,
    headers
  });

  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    if (response.status === 401 && !options.skipAuthRedirect) {
      clearSession();
    }
    const message = typeof payload === "string" ? payload : payload.message || payload.detail;
    throw new Error(message || "请求失败，请稍后再试。");
  }

  return payload;
}

async function serializeFiles(fileList, kind) {
  const files = Array.from(fileList || []);
  if (!files.length) {
    return [];
  }

  const totalSize = files.reduce((sum, file) => sum + file.size, 0);
  if (totalSize > FILE_LIMITS.total) {
    throw new Error("本次选择的文件总大小过大，请分批上传。");
  }

  return Promise.all(files.map((file) => serializeSingleFile(file, kind)));
}

async function serializeSingleFile(file, kind) {
  validateFile(file, kind);
  return {
    name: file.name,
    type: file.type,
    size: file.size,
    dataUrl: await readFileAsDataUrl(file)
  };
}

function validateFile(file, kind) {
  const expectedPrefix = `${kind}/`;
  if (!file.type.startsWith(expectedPrefix)) {
    throw new Error(`选择的文件中包含非${kind === "image" ? "图片" : "视频"}格式，请重新选择。`);
  }

  const limit = FILE_LIMITS[kind];
  if (file.size > limit) {
    throw new Error(`${file.name} 超出大小限制，请压缩后再上传。`);
  }
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error(`读取 ${file.name} 时失败。`));
    reader.readAsDataURL(file);
  });
}

function updateFileHint(input, hintElement, label, limit) {
  const files = Array.from(input.files || []);
  if (!files.length) {
    hintElement.textContent = label === "图片"
      ? "支持一次选择多张图片，单张建议不超过 8MB。"
      : "支持常见视频格式，单个建议不超过 35MB。";
    return;
  }

  const totalSizeMb = (files.reduce((sum, file) => sum + file.size, 0) / 1024 / 1024).toFixed(1);
  const oversizeCount = files.filter((file) => file.size > limit).length;

  hintElement.textContent = oversizeCount
    ? `已选 ${files.length} 个${label}，其中 ${oversizeCount} 个超出大小限制。`
    : `已选 ${files.length} 个${label}，总大小约 ${totalSizeMb}MB。`;
}

function splitListInput(value) {
  return String(value || "")
    .split(/[，,、；;]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function createEmptyRagState(archiveId = null) {
  return {
    archiveId,
    lastQuestion: "",
    answer: "",
    sources: [],
    workflow: []
  };
}

function showToast(message, isError = false) {
  elements.toast.textContent = message;
  elements.toast.className = `toast visible ${isError ? "error" : ""}`;

  window.clearTimeout(showToast.timer);
  showToast.timer = window.setTimeout(() => {
    elements.toast.className = "toast";
  }, 2600);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}

function escapeAttribute(value) {
  return escapeHtml(value).replaceAll("`", "&#96;");
}
