delete from assets;
delete from archive_timelines;
delete from archive_tags;
delete from archive_supporters;
delete from archives;
delete from app_users;

insert into archives (
    id, name, age, hometown, community, role, summary, wish, tone, created_at, updated_at
) values (
    'elder-demo-1',
    'Zhang Guilan',
    79,
    'Guangyuan, Sichuan',
    'Yulin Street, Chengdu',
    '退休纺织工人',
    '这是用于联调与回归测试的示例档案，展示摘要、标签、时间线和媒体数据在工作台中的呈现方式。',
    '把家常菜谱和家族记忆整理下来，留给下一代。',
    'amber',
    TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z',
    TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z'
);

insert into archive_tags (archive_id, tag) values
    ('elder-demo-1', '口述史'),
    ('elder-demo-1', '家庭记忆');

insert into archive_supporters (archive_id, supporter) values
    ('elder-demo-1', '社区社工'),
    ('elder-demo-1', '女儿');

insert into archive_timelines (
    id, archive_id, year_label, title, description, sort_order, created_at
) values
    (
        'timeline-demo-1',
        'elder-demo-1',
        '1964',
        '第一次进城工作',
        '离开家乡后，她开始一边工作一边写日记，记录新的城市生活。',
        1,
        TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z'
    ),
    (
        'timeline-demo-2',
        'elder-demo-1',
        '2024',
        '开始使用智能手机',
        '她逐渐学会扫码、视频通话，也开始尝试把老照片讲给家人听。',
        2,
        TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z'
    );
