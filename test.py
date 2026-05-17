import json
with open('data/output/courses_2026_1.json', 'r', encoding='utf-8') as f:
    data = json.load(f)
print('과목 수:', len(data))
print(data[0].get('과목명', ''))