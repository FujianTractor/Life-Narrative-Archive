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

    await fireEvent.update(view.getByLabelText("年份"), "1998");
    await fireEvent.update(view.getByLabelText("地点"), " 成都 ");
    await fireEvent.update(view.getByLabelText("标题"), " 搬到社区居住 ");
    await fireEvent.update(view.getByLabelText("描述"), " 开始在社区参加合唱和手工活动。 ");

    await fireEvent.click(view.getByRole("button", { name: "补录时间线" }));

    await waitFor(() => {
      expect(onAppendTimeline).toHaveBeenCalledWith({
        year: "1998",
        location: "成都",
        title: "搬到社区居住",
        description: "开始在社区参加合唱和手工活动。",
      });
    });

    expect(await view.findByText("时间线已追加到当前档案。")).toBeTruthy();
  });

  it("updates an existing timeline payload in editable mode", async () => {
    const onUpdateTimeline = vi.fn().mockResolvedValue(undefined);
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
      timeline: [
        {
          id: "timeline-1",
          year: 1998,
          location: "广元",
          title: "旧标题",
          description: "旧描述",
        },
      ],
      assets: { images: [], videos: [] },
    };

    const view = render(ArchiveDetailPanel, {
      props: {
        archive,
        editable: true,
        onUpdateTimeline,
      },
    });

    await fireEvent.click(view.getByRole("button", { name: "编辑" }));
    await fireEvent.update(view.getByLabelText("年份"), "2001");
    await fireEvent.update(view.getByLabelText("地点"), " 成都 ");
    await fireEvent.update(view.getByLabelText("标题"), " 新标题 ");
    await fireEvent.update(view.getByLabelText("描述"), " 新描述 ");
    await fireEvent.click(view.getByRole("button", { name: "更新时间线" }));

    await waitFor(() => {
      expect(onUpdateTimeline).toHaveBeenCalledWith("timeline-1", {
        year: "2001",
        location: "成都",
        title: "新标题",
        description: "新描述",
      });
    });
  });

  it("deletes an existing timeline entry in editable mode", async () => {
    const onDeleteTimeline = vi.fn().mockResolvedValue(undefined);
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
      timeline: [
        {
          id: "timeline-1",
          year: 1998,
          location: "成都",
          title: "搬到社区居住",
          description: "开始在社区参加活动。",
        },
      ],
      assets: { images: [], videos: [] },
    };

    const view = render(ArchiveDetailPanel, {
      props: {
        archive,
        editable: true,
        onDeleteTimeline,
      },
    });

    await fireEvent.click(view.getByRole("button", { name: "删除" }));

    await waitFor(() => {
      expect(onDeleteTimeline).toHaveBeenCalledWith("timeline-1");
    });
  });

  it("renders timeline entries in chronological order", () => {
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
      timeline: [
        {
          id: "timeline-2024",
          year: 2024,
          location: "成都",
          title: "后加入的晚年节点",
          description: "晚年节点",
        },
        {
          id: "timeline-1964",
          year: 1964,
          location: "广元",
          title: "后补的早年节点",
          description: "早年节点",
        },
      ],
      assets: { images: [], videos: [] },
    };

    const view = render(ArchiveDetailPanel, {
      props: {
        archive,
      },
    });

    expect(view.container.textContent?.indexOf("1964")).toBeLessThan(view.container.textContent?.indexOf("2024") ?? 0);
  });
});
