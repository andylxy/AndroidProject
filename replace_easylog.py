import os

root_dir = r"d:\git\app\AndroidProject-old\app\src\main\java"

for root, dirs, files in os.walk(root_dir):
    for file in files:
        if file.endswith(".java"):
            file_path = os.path.join(root, file)
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                if 'import com.hjq.http.EasyLog;' in content:
                    print(f"Replacing in {file_path}")
                    new_content = content.replace('import com.hjq.http.EasyLog;', 'import run.yigou.gxzy.utils.EasyLog;')
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
            except Exception as e:
                print(f"Error processing {file_path}: {e}")
