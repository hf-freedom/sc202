<template>
  <div id="app">
    <el-container>
      <el-header class="header">
        <div class="title">多级报销审批系统</div>
        <div class="user-info">
          <el-dropdown @command="handleUserChange">
            <span class="el-dropdown-link">
              当前用户：{{ currentUser.name }}<i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item v-for="user in userList" :key="user.id" :command="user.id">
                {{ user.name }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" class="aside">
          <el-menu :default-active="activeMenu" router class="menu">
            <el-menu-item index="/">
              <i class="el-icon-document"></i>
              <span>我的报销</span>
            </el-menu-item>
            <el-menu-item index="/create">
              <i class="el-icon-edit"></i>
              <span>新建报销</span>
            </el-menu-item>
            <el-menu-item index="/approval">
              <i class="el-icon-s-check"></i>
              <span>待我审批</span>
            </el-menu-item>
            <el-menu-item index="/budget">
              <i class="el-icon-s-data"></i>
              <span>预算查看</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="main">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      currentUser: {
        id: 'user_emp1',
        name: '员工张三'
      },
      userList: []
    }
  },
  computed: {
    activeMenu() {
      return this.$route.path
    }
  },
  created() {
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      try {
        const res = await this.$axios.get('/api/users')
        if (res.data.success) {
          this.userList = res.data.data
          const savedUserId = localStorage.getItem('currentUserId')
          if (savedUserId) {
            const user = this.userList.find(u => u.id === savedUserId)
            if (user) {
              this.currentUser = user
            }
          }
        }
      } catch (e) {
        console.error('加载用户失败', e)
      }
    },
    handleUserChange(userId) {
      const user = this.userList.find(u => u.id === userId)
      if (user) {
        this.currentUser = user
        localStorage.setItem('currentUserId', userId)
        this.$emit('user-change', user)
        this.$router.go(0)
      }
    }
  }
}
</script>

<style>
html, body, #app, .el-container {
  margin: 0;
  padding: 0;
  height: 100%;
}

.header {
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header .title {
  font-size: 18px;
  font-weight: bold;
}

.header .user-info .el-dropdown-link {
  cursor: pointer;
  color: white;
}

.aside {
  background-color: #F5F7FA;
}

.menu {
  border-right: none;
}

.main {
  background-color: white;
  padding: 20px;
}
</style>
