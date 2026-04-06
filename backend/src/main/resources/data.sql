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
    'Retired textile worker',
    'Persistent backend sample archive used for integration and regression checks.',
    'Preserve recipes and family memories for younger generations.',
    'amber',
    TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z',
    TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z'
);

insert into archive_tags (archive_id, tag) values
    ('elder-demo-1', 'oral history'),
    ('elder-demo-1', 'family memories');

insert into archive_supporters (archive_id, supporter) values
    ('elder-demo-1', 'community worker'),
    ('elder-demo-1', 'daughter');

insert into archive_timelines (
    id, archive_id, year_label, title, description, sort_order, created_at
) values
    (
        'timeline-demo-1',
        'elder-demo-1',
        '1964',
        'First trip to the city',
        'She started keeping a diary after leaving her hometown.',
        1,
        TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z'
    ),
    (
        'timeline-demo-2',
        'elder-demo-1',
        '2024',
        'Started using a smartphone',
        'She gradually learned how to scan codes and make video calls.',
        2,
        TIMESTAMP WITH TIME ZONE '2026-04-05T00:00:00Z'
    );