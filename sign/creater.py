import os
import base64
import getpass
import sys
import subprocess
from pathlib import Path

def run_command(command):
    """运行系统命令并返回结果"""
    try:
        result = subprocess.run(command, shell=True, check=True, 
                              text=True, capture_output=True)
        return result.stdout
    except subprocess.CalledProcessError as e:
        print(f"命令执行失败: {e}")
        print(f"错误输出: {e.stderr}")
        sys.exit(1)

def create_keystore():
    """创建keystore文件"""
    print("===== 开始创建签名密钥 =====")
    keystore_path = "my-release-key.jks"
    
    # 检查keystore是否已存在
    if os.path.exists(keystore_path):
        overwrite = input(f"{keystore_path} 已存在，是否覆盖? (y/n): ").lower() == 'y'
        if not overwrite:
            print(f"使用现有的 {keystore_path}")
            key_password = getpass.getpass("请输入现有keystore密码: ")
            key_alias = input("请输入现有密钥别名 [my-alias]: ") or "my-alias"
            return keystore_path, key_password, key_alias
    
    # 收集创建keystore所需信息
    key_password = getpass.getpass("请输入keystore密码: ")
    key_alias = input("请输入密钥别名 [my-alias]: ") or "my-alias"
    
    # 收集证书信息
    print("\n请输入证书信息:")
    name = input("名字与姓氏 (CN) [Android]: ") or "Android"
    org_unit = input("组织单位 (OU) [Development]: ") or "Development"
    org = input("组织名称 (O) [Your Organization]: ") or "Your Organization"
    city = input("城市 (L) [Your City]: ") or "Your City"
    state = input("省/市/自治区 (ST) [Your State]: ") or "Your State"
    country = input("国家/地区代码 (C) [CN]: ") or "CN"
    
    # 构建dname参数
    dname = f"CN={name}, OU={org_unit}, O={org}, L={city}, ST={state}, C={country}"
    
    # 构建keytool命令
    cmd = (f'keytool -genkey -v -keystore {keystore_path} -keyalg RSA -keysize 2048 '
           f'-validity 10000 -alias {key_alias} -storepass {key_password} -keypass {key_password} '
           f'-dname "{dname}"')
    
    # 执行keytool命令
    print("\n正在生成密钥，请稍候...")
    try:
        output = run_command(cmd)
        print("密钥生成成功!")
        return keystore_path, key_password, key_alias
    except Exception as e:
        print(f"生成密钥时出错: {e}")
        sys.exit(1)

def create_signing_properties(keystore_path, keystore_pwd=None, keystore_alias=None):
    """创建签名配置文件及base64编码"""
    print("\n===== 开始创建签名配置 =====")
    
    # 如果没有提供密码和别名，则从用户输入获取
    if keystore_pwd is None:
        keystore_pwd = getpass.getpass("请输入keystore密码: ")
    
    # 如果没有提供别名，则从用户输入获取
    if keystore_alias is None:
        keystore_alias = input("请输入密钥别名 [my-alias]: ") or "my-alias"
    else:
        print(f"使用密钥别名: {keystore_alias}")
    
    # 获取别名密码
    alias_pwd_same = input("别名密码是否与keystore密码相同? (y/n) [y]: ").lower() != 'n'
    if alias_pwd_same:
        alias_pwd = keystore_pwd
    else:
        alias_pwd = getpass.getpass("请输入别名密码: ")
    
    # 创建signing.properties文件内容
    properties_content = f"""keystore.path={keystore_path}
keystore.pwd={keystore_pwd}
keystore.alias={keystore_alias}
keystore.alias_pwd={alias_pwd}
"""
    
    # 确保sign目录存在
    sign_dir = Path("sign")
    sign_dir.mkdir(exist_ok=True)
    
    # 写入signing.properties文件
    properties_path = sign_dir / "signing.properties"
    with open(properties_path, "w") as f:
        f.write(properties_content)
    print(f"signing.properties文件已成功创建: {properties_path}")
    
    # 生成base64编码文件
    create_base64_files(keystore_path, properties_path)

def create_base64_files(keystore_path, properties_path):
    """生成base64编码文件"""
    print("\n===== 生成GitHub Actions所需的base64编码文件 =====")
    
    # 确保sign目录存在
    sign_dir = Path("sign")
    sign_dir.mkdir(exist_ok=True)
    
    # 生成base64编码的signing.properties内容
    with open(properties_path, "rb") as f:
        properties_base64 = base64.b64encode(f.read()).decode('utf-8')
    
    # 生成base64编码的keystore内容
    with open(keystore_path, "rb") as f:
        keystore_base64 = base64.b64encode(f.read()).decode('utf-8')
    
    # 创建base64编码文件
    properties_base64_path = sign_dir / "properties_base64.txt"
    with open(properties_base64_path, "w") as f:
        f.write(properties_base64)
    
    key_base64_path = sign_dir / "key_base64.txt"
    with open(key_base64_path, "w") as f:
        f.write(keystore_base64)
    
    print("\n已为GitHub Actions创建所需的base64编码文件:")
    print(f"1. {properties_base64_path} - 包含SIGNING_PROPERTIES的base64编码内容")
    print(f"2. {key_base64_path} - 包含SIGN_KEY的base64编码内容")
    print("\n请将这些文件的内容添加到GitHub仓库的Secrets中:")
    print("SIGNING_PROPERTIES: properties_base64.txt的内容")
    print("SIGN_KEY: key_base64.txt的内容")

def main():
    print("===== Android应用签名一站式配置工具 =====\n")
    
    # 第一步：创建keystore
    keystore_path, key_password, key_alias = create_keystore()
    
    # 第二步：创建signing.properties和base64编码文件
    create_signing_properties(keystore_path, key_password, key_alias)
    
    print("\n===== 全部完成! =====")
    print("请将生成的base64文件内容添加到GitHub仓库的Secrets中")
    print("这样可以确保GitHub Actions构建的应用与本地构建签名一致，支持相互覆盖安装")

if __name__ == "__main__":
    main()