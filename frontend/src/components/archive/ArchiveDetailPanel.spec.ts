import { fireEvent, render, waitFor } from "@testing-library/vue";
import { describe, expect, it, vi } from "vitest";

import ArchiveDetailPanel from "./ArchiveDetailPanel.vue";

describe("ArchiveDetailPanel", () => {
  it("submits a trimmed timeline payload in editable mode", async () => {
    const onAppendTimeline = vi.fn().mockResolvedValue(undefined);
    const archive = {
      id: "elder-1",
      name: "张桂兰",
      age: 79,
      hometown: "四川广元",
      community: "成都玉林街道",
      role: "退休纺织工人",
      summary: "示例摘要",
      wish: "",
      tags: ["口述史"],
      supporters: [],
      tone: "amber",
      updatedAt: "2026-04-05T00:00:00Z",
      timeline: [],
      assets: { images: [], videos: [] },
    };

    const view = render(ArchiveDetailPanel, {
      props: {
        archive,
        editable: true,
        onAppendTimeline,
      },
    });

    await fireEvent.update(view.getByLabelText("年份"), " 1998 ");
    await fireEvent.update(view.getByLabelText("标题"), " 搬到社区居住 ");
    await fireEvent.update(view.getByLabelText("描述"), " 开始在社区参加合唱和手工活动。 ");

    await fireEvent.click(view.getByRole("button", { name: "补录时间线" }));

    await waitFor(() => {
      expect(onAppendTimeline).toHaveBeenCalledWith({
        year: "1998",
        title: "搬到社区居住",
        description: "开始在社区参加合唱和手工活动。",
      });
    });

    expect(await view.findByText("时间线已追加到当前档案。")).toBeTruthy();
  });
});
