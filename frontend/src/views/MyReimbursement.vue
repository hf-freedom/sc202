<template>
  <div class="my-reimbursement">
    <el-card>
      <div slot="header">
        <span>我的报销申请</span>
      </div>
      <el-table :data="reimbursementList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="报销单号" width="260"></el-table-column>
        <el-table-column prop="title" label="标题" width="150"></el-table-column>
        <el-table-column prop="amount" label="金额(元)" width="120">
          <template slot-scope="scope">
            <span style="color: #409EFF; font-weight: bold">{{ scope.row.amount }}</span>
            <el-tag v-if="Number(scope.row.amount) < 5000" type="success" size="mini" style="margin-left: 5px">小额</el-tag>
            <el-tag v-else type="warning" size="mini" style="margin-left: 5px">大额</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="invoiceNumber" label="发票号" width="130"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批链" width="350">
          <template slot-scope="scope">
            <div v-if="scope.row.approvalTrail && scope.row.approvalTrail.length > 0" class="approval-chain">
              <span 
                v-for="(node, index) in sortedNodes(scope.row.approvalTrail)" 
                :key="node.id" 
                class="chain-node"
                :class="getApprovalNodeClass(node.status)"
              >
                <i :class="getApprovalNodeIcon(node.status)"></i>
                <span class="node-name">{{ getApprovalLevelShortText(node.approvalLevel) }}</span>
                <span class="node-approver" :title="node.approverName">{{ node.approverName }}</span>
              </span>
            </div>
            <el-tag v-else type="info" size="mini">未提交</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160"></el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="viewDetail(scope.row.id)">详情</el-button>
            <el-button v-if="scope.row.status === 'DRAFT'" type="text" size="small" @click="editReimbursement(scope.row.id)">编辑</el-button>
            <el-button v-if="scope.row.status === 'DRAFT'" type="text" size="small" @click="submitReimbursement(scope.row.id)">提交</el-button>
            <el-button v-if="scope.row.status === 'REJECTED_RESUBMIT'" type="text" size="small" @click="resubmitReimbursement(scope.row.id)">重提</el-button>
            <el-button v-if="scope.row.status === 'PENDING'" type="text" size="small" @click="withdrawReimbursement(scope.row.id)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'MyReimbursement',
  data() {
    return {
      reimbursementList: [],
      loading: false
    }
  },
  created() {
    this.loadReimbursements()
  },
  methods: {
    getCurrentUserId() {
      return localStorage.getItem('currentUserId') || 'user_emp1'
    },
    async loadReimbursements() {
      this.loading = true
      try {
        const userId = this.getCurrentUserId()
        const res = await this.$axios.get(`/api/reimbursements/applicant/${userId}`)
        if (res.data.success) {
          this.reimbursementList = res.data.data
        }
      } catch (e) {
        this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    sortedNodes(nodes) {
      return [...nodes].sort((a, b) => a.nodeOrder - b.nodeOrder)
    },
    getStatusType(status) {
      const statusMap = {
        'DRAFT': 'info',
        'PENDING': 'warning',
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'REJECTED_RESUBMIT': 'danger',
        'TRANSFERRED': 'warning',
        'WITHDRAWN': 'info',
        'COMPLETED': 'success'
      }
      return statusMap[status] || 'info'
    },
    getStatusText(status) {
      const statusMap = {
        'DRAFT': '草稿',
        'PENDING': '待审批',
        'APPROVED': '已通过',
        'REJECTED': '已驳回',
        'REJECTED_RESUBMIT': '驳回待重提',
        'TRANSFERRED': '已转交',
        'WITHDRAWN': '已撤回',
        'COMPLETED': '已完成'
      }
      return statusMap[status] || status
    },
    getApprovalNodeClass(status) {
      const classMap = {
        'PENDING': 'pending',
        'APPROVED': 'approved',
        'REJECTED': 'rejected',
        'TRANSFERRED': 'transferred',
        'WITHDRAWN': 'withdrawn',
        'SKIPPED': 'skipped'
      }
      return classMap[status] || 'pending'
    },
    getApprovalNodeIcon(status) {
      const iconMap = {
        'PENDING': 'el-icon-time',
        'APPROVED': 'el-icon-success',
        'REJECTED': 'el-icon-error',
        'TRANSFERRED': 'el-icon-right',
        'WITHDRAWN': 'el-icon-refresh-left',
        'SKIPPED': 'el-icon-minus'
      }
      return iconMap[status] || 'el-icon-time'
    },
    getApprovalLevelShortText(level) {
      const levelMap = {
        'DIRECT_MANAGER': '直属',
        'DEPARTMENT_HEAD': '部门',
        'FINANCE': '财务'
      }
      return levelMap[level] || level
    },
    viewDetail(id) {
      this.$router.push(`/detail/${id}`)
    },
    editReimbursement(id) {
      this.$router.push(`/edit/${id}`)
    },
    resubmitReimbursement(id) {
      this.$router.push(`/resubmit/${id}`)
    },
    async submitReimbursement(id) {
      try {
        const res = await this.$axios.post(`/api/reimbursements/${id}/submit`)
        if (res.data.success) {
          this.$message.success('提交成功')
          this.loadReimbursements()
        } else {
          this.$message.error(res.data.message || '提交失败')
        }
      } catch (e) {
        this.$message.error('提交失败')
      }
    },
    async withdrawReimbursement(id) {
      this.$confirm('确定要撤回报销申请吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const userId = this.getCurrentUserId()
          const res = await this.$axios.post(`/api/reimbursements/${id}/withdraw`, null, {
            params: { applicantId: userId, comment: '用户撤回' }
          })
          if (res.data.success) {
            this.$message.success('撤回成功')
            this.loadReimbursements()
          } else {
            this.$message.error(res.data.message || '撤回失败')
          }
        } catch (e) {
          this.$message.error('撤回失败')
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.approval-chain {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  overflow-x: auto;
}

.chain-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 8px;
  border-radius: 4px;
  margin-right: 4px;
  font-size: 12px;
  min-width: 60px;
}

.chain-node i {
  font-size: 16px;
  margin-bottom: 2px;
}

.chain-node .node-name {
  font-weight: bold;
  font-size: 11px;
}

.chain-node .node-approver {
  font-size: 10px;
  color: #606266;
  max-width: 50px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chain-node.pending {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.chain-node.approved {
  background-color: #f0f9eb;
  color: #67c23a;
}

.chain-node.rejected {
  background-color: #fef0f0;
  color: #f56c6c;
}

.chain-node.transferred {
  background-color: #ecf5ff;
  color: #409eff;
}

.chain-node.withdrawn {
  background-color: #f4f4f5;
  color: #909399;
}

.chain-node.skipped {
  background-color: #f4f4f5;
  color: #c0c4cc;
  opacity: 0.7;
}
</style>
