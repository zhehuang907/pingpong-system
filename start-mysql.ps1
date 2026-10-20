# 启动 MySQL 服务脚本
Write-Host "正在启动 MySQL 服务..." -ForegroundColor Green

try {
    # 尝试启动 MySQL 服务
    Start-Service MySQL
    Write-Host "✓ MySQL 服务启动成功！" -ForegroundColor Green
    
    # 验证服务状态
    $service = Get-Service MySQL
    Write-Host "服务状态: $($service.Status)" -ForegroundColor Cyan
} catch {
    Write-Host "✗ 启动失败: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "请尝试以下方法：" -ForegroundColor Yellow
    Write-Host "1. 右键点击此文件，选择'以管理员身份运行'" -ForegroundColor White
    Write-Host "2. 或者手动打开 services.msc，找到 MySQL 服务并启动" -ForegroundColor White
}

Write-Host ""
Write-Host "按任意键退出..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
