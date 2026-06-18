import os, re

replacements = [
    ('schema = "auth_schema"',         'schema = "auth"'),
    ('schema = "expense_schema"',      'schema = "expense"'),
    ('schema = "user_schema"',         'schema = "users"'),
    ('schema = "fraud_schema"',        'schema = "fraud"'),
    ('schema = "notification_schema"', 'schema = "notification"'),
    ('schema = "audit_schema"',        'schema = "audit"'),
    ('schema = "analytics_schema"',    'schema = "analytics"'),
]

services_dir = os.path.join(os.path.dirname(__file__), 'services')
changed = 0

for root, dirs, files in os.walk(services_dir):
    # Skip compiled output
    dirs[:] = [d for d in dirs if d != 'target']
    for fname in files:
        if not fname.endswith('.java'):
            continue
        path = os.path.join(root, fname)
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        new_content = content
        for old, new in replacements:
            new_content = new_content.replace(old, new)
        if new_content != content:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f'Fixed: {path}')
            changed += 1

print(f'\nTotal files changed: {changed}')
