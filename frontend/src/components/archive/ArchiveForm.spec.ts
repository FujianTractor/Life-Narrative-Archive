import { fireEvent, render, waitFor } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";

import ArchiveForm from "./ArchiveForm.vue";

describe("ArchiveForm", () => {
  it("normalizes Chinese and English separators before submitting", async () => {
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
    await fireEvent.update(view.getByLabelText("摘要"), " 想把教学故事整理成长期档案。 ");
    await fireEvent.update(view.getByLabelText("愿望"), " 把旧照片讲给孙辈听。 ");
    await fireEvent.update(view.getByLabelText("标签"), "教育记忆，代际交流, 社区志愿");
    await fireEvent.update(view.getByLabelText("支持者"), "外孙女、社区志愿者");

    await fireEvent.click(view.getByRole("button", { name: "创建档案" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        name: "李素云",
        age: 74,
        hometown: "内江，四川",
        community: "望江路社区",
        role: "退休小学教师",
        summary: "想把教学故事整理成长期档案。",
        wish: "把旧照片讲给孙辈听。",
        tags: ["教育记忆", "代际交流", "社区志愿"],
        supporters: ["外孙女", "社区志愿者"],
        tone: "amber",
      });
    });

    expect(await view.findByText("档案已创建并载入工作台。") ).toBeTruthy();
  });
});