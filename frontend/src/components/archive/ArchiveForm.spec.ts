import { fireEvent, render, waitFor } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";

import ArchiveForm from "./ArchiveForm.vue";

describe("ArchiveForm", () => {
  it("normalizes separators and forwards selected summary images", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const view = render(ArchiveForm, {
      props: {
        onSubmit,
      },
    });

    await fireEvent.update(view.getByLabelText("姓名"), "  李素云 ");
    await fireEvent.update(view.getByLabelText("年龄"), "74");
    await fireEvent.update(view.getByLabelText("社区"), " 望江路社区 ");
    await fireEvent.update(view.getByLabelText("籍贯"), " 内江，四川 ");
    await fireEvent.update(view.getByLabelText("角色"), " 退休小学教师 ");
    await fireEvent.update(view.getByLabelText("摘要"), " 想把教学故事整理成长久可补充的档案。 ");
    await fireEvent.update(view.getByLabelText("标签"), "口述史，家庭记忆；社区融入；教育回忆");

    const imageInput = view.getByLabelText("摘要图片") as HTMLInputElement;
    const files = [
      new File(["first"], "photo-a.jpg", { type: "image/jpeg" }),
      new File(["second"], "photo-b.png", { type: "image/png" }),
    ];
    await fireEvent.change(imageInput, { target: { files } });

    await fireEvent.click(view.getByRole("button", { name: "创建档案" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        {
          name: "李素云",
          age: 74,
          hometown: "内江，四川",
          community: "望江路社区",
          role: "退休小学教师",
          summary: "想把教学故事整理成长久可补充的档案。",
          wish: "",
          tags: ["口述史", "家庭记忆", "社区融入", "教育回忆"],
          supporters: [],
          tone: "amber",
        },
        files,
      );
    });
  });
});
