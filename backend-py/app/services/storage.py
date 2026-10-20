"""文件存储服务。

由 Java ``ImageStorageServiceImpl`` 移植：头像与装备图片上传，
校验扩展名与体积，按子目录归档，返回可访问 URL。
"""

from __future__ import annotations

import os
import uuid
from typing import Optional

from fastapi import UploadFile

from app.core.config import settings
from app.core.exceptions import BusinessException


class ImageStorageService:
    """图片存储服务。"""

    def __init__(self, upload_dir: Optional[str] = None):
        self.upload_dir = upload_dir or settings.upload_dir

    def save(self, file: UploadFile, subdir: str = "equipment") -> str:
        """保存上传文件。

        Args:
            file: FastAPI 上传文件对象。
            subdir: 归档子目录（avatar / equipment）。

        Returns:
            可访问相对路径，如 ``/uploads/equipment/xxx.png``。
        """
        filename = file.filename or ""
        ext = os.path.splitext(filename)[1].lower()
        if ext not in settings.allowed_image_exts:
            raise BusinessException(
                f"不支持的图片格式，仅允许 {', '.join(settings.allowed_image_exts)}"
            )

        target_dir = os.path.join(self.upload_dir, subdir)
        os.makedirs(target_dir, exist_ok=True)

        new_name = f"{uuid.uuid4().hex}{ext}"
        target_path = os.path.join(target_dir, new_name)

        content = file.file.read()
        if len(content) > settings.upload_max_size:
            raise BusinessException(
                f"文件超过大小限制（最大 {settings.upload_max_size // 1024 // 1024}MB）"
            )

        with open(target_path, "wb") as f:
            f.write(content)

        return f"/uploads/{subdir}/{new_name}"

    def delete(self, relative_url: str) -> bool:
        """删除已上传文件（按相对 URL 定位）。"""
        if not relative_url or not relative_url.startswith("/uploads/"):
            return False
        path = os.path.join(self.upload_dir, relative_url[len("/uploads/") :])
        try:
            if os.path.exists(path):
                os.remove(path)
                return True
        except OSError:
            return False
        return False
