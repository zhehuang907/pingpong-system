<template>
  <div class="footer">
    <div class="footer-content">
      <div class="footer-section">
        <h4>关于我们</h4>
        <p>专业的乒乓球装备推荐平台</p>
        <p>为乒乓球爱好者提供精准装备选择建议</p>
      </div>
      
      <div class="footer-section">
        <h4>快速链接</h4>
        <p><a href="/">首页</a></p>
        <p><a href="/equipment-list">装备列表</a></p>
        <p><a href="/login">登录</a></p>
      </div>
      
      <div class="footer-section">
        <h4>联系方式</h4>
        <p>邮箱：contact@pingpong.com</p>
        <p>电话：400-123-4567</p>
      </div>
    </div>
    
    <div class="copyright">
      © 2026 乒乓球装备推荐系统. 版权所有.
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped lang="scss">
.footer {
  background: #2c3e50;
  color: white;
  padding: 40px 0 20px;
  margin-top: 60px;

  .footer-content {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 20px;
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 40px;

    .footer-section {
      h4 {
        color: #ecf0f1;
        margin-bottom: 15px;
      }

      p {
        color: #bdc3c7;
        margin-bottom: 8px;

        a {
          color: #bdc3c7;
          text-decoration: none;

          &:hover {
            color: #3498db;
          }
        }
      }
    }
  }

  .copyright {
    text-align: center;
    padding-top: 30px;
    margin-top: 30px;
    border-top: 1px solid #34495e;
    color: #95a5a6;
  }
}
</style>
