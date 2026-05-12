<template>
  <div class="budget-view">
    <el-card>
      <div slot="header">
        <span>部门预算</span>
      </div>
      <el-table :data="budgetList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="departmentName" label="部门名称" width="150"></el-table-column>
        <el-table-column prop="totalBudget" label="总预算(元)" width="150">
          <template slot-scope="scope">
            <span>{{ formatNumber(scope.row.totalBudget) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="usedBudget" label="已使用(元)" width="150">
          <template slot-scope="scope">
            <span style="color: #F56C6C">{{ formatNumber(scope.row.usedBudget) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reservedBudget" label="已占用(元)" width="150">
          <template slot-scope="scope">
            <span style="color: #E6A23C">{{ formatNumber(scope.row.reservedBudget) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可用预算(元)" width="150">
          <template slot-scope="scope">
            <span style="color: #67C23A; font-weight: bold">{{ formatNumber(scope.row.availableBudget) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用率" width="200">
          <template slot-scope="scope">
            <el-progress :percentage="getUsagePercent(scope.row)" :status="getProgressStatus(scope.row)"></el-progress>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'BudgetView',
  data() {
    return {
      budgetList: [],
      loading: false
    }
  },
  created() {
    this.loadBudgets()
  },
  methods: {
    async loadBudgets() {
      this.loading = true
      try {
        const res = await this.$axios.get('/api/budgets')
        if (res.data.success) {
          this.budgetList = res.data.data
        }
      } catch (e) {
        this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    formatNumber(num) {
      if (num == null) return '0.00'
      return Number(num).toFixed(2)
    },
    getUsagePercent(budget) {
      if (!budget.totalBudget || budget.totalBudget == 0) return 0
      const used = Number(budget.usedBudget) + Number(budget.reservedBudget)
      const total = Number(budget.totalBudget)
      return Math.min(Math.round((used / total) * 100), 100)
    },
    getProgressStatus(budget) {
      const percent = this.getUsagePercent(budget)
      if (percent >= 90) return 'exception'
      if (percent >= 70) return 'warning'
      return null
    }
  }
}
</script>
