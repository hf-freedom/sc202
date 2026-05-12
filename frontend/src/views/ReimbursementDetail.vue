<template>
  <div class="reimbursement-detail">
    <el-card v-loading="loading">
      <div slot="header">
        <span>报销单详情</span>
        <el-button type="text" @click="goBack" style="float: right;">返回</el-button>
      </div>
      
      <div v-if="reimburs">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报销单号">{{ reimburs.id }}</el-descriptions-item>
          <el-descriptions-item label="版本">V{{ reimburs.version }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ reimburs.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ reimburs.departmentName }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ reimburs.title }}</el-descriptions-item>
          <el-descriptions-item label="金额">
            <span style="color: #409EFF; font-weight: bold; font-size: 16px;">¥{{ reimburs.amount }}</span>
            <el-tag v-if="Number(reimburs.amount) < 5000" type="success" size="mini" style="margin-left: 10px;">小额 - 仅直属领导审批</el-tag>
            <el-tag v-else type="warning" size="mini" style="margin-left: 10px;">大额 - 三级审批（直属→部门→财务）</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(reimburs.status)">{{ getStatusText(reimburs.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="当前审批人">{{ reimburs.currentApproverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发票号">{{ reimburs.invoiceNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ reimburs.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ reimburs.submittedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ reimburs.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ reimburs.description || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="reimburs.rejectReason" label="驳回原因" :span="2">
            <span style="color: #F56C6C">{{ reimburs.rejectReason }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="reimburs.parentId" label="关联单号" :span="2">{{ reimburs.parentId }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">审批进度概览</el-divider>
        
        <div class="approval-overview">
          <div 
            v-for="(level, index) in approvalLevels" 
            :key="level.value"
            class="level-item"
            :class="getLevelStatusClass(level.value)"
          >
            <div class="level-icon">
              <i :class="getLevelIcon(level.value)"></i>
            </div>
            <div class="level-info">
              <div class="level-name">{{ level.label }}</div>
              <div class="level-approver">{{ getLevelApprover(level.value) }}</div>
            </div>
            <div v-if="index < approvalLevels.length - 1" class="level-arrow">
              <i class="el-icon-arrow-right"></i>
            </div>
          </div>
        </div>

        <el-divider content-position="left">详细审批轨迹</el-divider>
        
        <el-timeline v-if="reimburs.approvalTrail && reimburs.approvalTrail.length > 0">
          <el-timeline-item 
            v-for="(node, index) in sortedNodes" 
            :key="node.id"
            :timestamp="node.updatedAt"
            :type="getTimelineType(node.status)"
            :icon="getTimelineIcon(node.status)"
            placement="top"
            :color="getTimelineColor(node.status)"
          >
            <el-card class="timeline-card" shadow="hover">
              <div class="timeline-header">
                <span class="timeline-level">{{ getApprovalLevelText(node.approvalLevel) }}</span>
                <span class="timeline-approver">{{ node.approverName }}</span>
                <el-tag :type="getStatusType(node.status)" size="mini">
                  {{ getNodeStatusText(node.status) }}
                </el-tag>
                <el-tag 
                  v-if="isAutoUpgrade(node)" 
                  type="danger" 
                  size="mini" 
                  style="margin-left: 10px;"
                >
                  <i class="el-icon-alarm-clock"></i> 自动升级
                </el-tag>
              </div>
              <div v-if="node.comment" class="timeline-comment">
                <i class="el-icon-chat-dot-round"></i> {{ node.comment }}
              </div>
              <div v-if="node.transferredReason" class="timeline-transfer">
                <i :class="isAutoUpgrade(node) ? 'el-icon-alarm-clock' : 'el-icon-right'"></i> 
                {{ isAutoUpgrade(node) ? '自动升级原因：' : '转交原因：' }}{{ node.transferredReason }}
              </div>
              <div v-if="node.transferredFromId && node.transferredToId" class="timeline-transfer-detail">
                从 <strong>{{ getUserName(node.transferredFromId) }}</strong> 转交给 <strong>{{ getUserName(node.transferredToId) }}</strong>
              </div>
              <div class="timeline-time">
                <i class="el-icon-time"></i> 处理时间：{{ node.updatedAt }}
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无审批记录"></el-empty>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'ReimbursementDetail',
  data() {
    return {
      reimburs: null,
      loading: false,
      userMap: {}
    }
  },
  computed: {
    sortedNodes() {
      if (!this.reimburs || !this.reimburs.approvalTrail) return []
      return [...this.reimburs.approvalTrail].sort((a, b) => {
        if (a.nodeOrder !== b.nodeOrder) return a.nodeOrder - b.nodeOrder
        return new Date(a.createdAt) - new Date(b.createdAt)
      })
    },
    approvalLevels() {
      if (!this.reimburs) return []
      if (Number(this.reimburs.amount) < 5000) {
        return [{ value: 'DIRECT_MANAGER', label: '直属领导审批' }]
      }
      return [
        { value: 'DIRECT_MANAGER', label: '直属领导审批' },
        { value: 'DEPARTMENT_HEAD', label: '部门负责人审批' },
        { value: 'FINANCE', label: '财务审批' }
      ]
    }
  },
  created() {
    this.loadReimbursement()
    this.loadUsers()
  },
  methods: {
    async loadUsers() {
      try {
        const res = await this.$axios.get('/api/users')
        if (res.data.success) {
          res.data.data.forEach(user => {
            this.userMap[user.id] = user.name
          })
        }
      } catch (e) {
        console.error('加载用户失败', e)
      }
    },
    getUserName(userId) {
      return this.userMap[userId] || userId
    },
    async loadReimbursement() {
      this.loading = true
      try {
        const id = this.$route.params.id
        const res = await this.$axios.get(`/api/reimbursements/${id}`)
        if (res.data.success) {
          this.reimburs = res.data.data
        }
      } catch (e) {
        this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    goBack() {
      this.$router.go(-1)
    },
    getLevelStatusClass(level) {
      const node = this.getNodeByLevel(level)
      if (!node) return 'pending'
      if (node.status === 'APPROVED') return 'approved'
      if (node.status === 'REJECTED') return 'rejected'
      if (node.status === 'TRANSFERRED') return 'transferred'
      if (node.status === 'SKIPPED') return 'skipped'
      return 'pending'
    },
    getLevelIcon(level) {
      const node = this.getNodeByLevel(level)
      if (!node) return 'el-icon-time'
      if (node.status === 'APPROVED') return 'el-icon-circle-check'
      if (node.status === 'REJECTED') return 'el-icon-circle-close'
      if (node.status === 'TRANSFERRED') return 'el-icon-right'
      if (node.status === 'SKIPPED') return 'el-icon-minus'
      return 'el-icon-time'
    },
    getLevelApprover(level) {
      const node = this.getNodeByLevel(level)
      if (!node) return '待进入'
      return node.approverName
    },
    getNodeByLevel(level) {
      if (!this.reimburs || !this.reimburs.approvalTrail) return null
      return this.reimburs.approvalTrail.find(n => n.approvalLevel === level && n.status !== 'SKIPPED')
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
        'COMPLETED': 'success',
        'SKIPPED': 'info'
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
        'COMPLETED': '已完成',
        'SKIPPED': '已跳过'
      }
      return statusMap[status] || status
    },
    getNodeStatusText(status) {
      const statusMap = {
        'PENDING': '待审批',
        'APPROVED': '通过',
        'REJECTED': '驳回',
        'TRANSFERRED': '转交',
        'WITHDRAWN': '撤回',
        'SKIPPED': '跳过'
      }
      return statusMap[status] || status
    },
    getApprovalLevelText(level) {
      const levelMap = {
        'DIRECT_MANAGER': '直属领导审批',
        'DEPARTMENT_HEAD': '部门负责人审批',
        'FINANCE': '财务审批'
      }
      return levelMap[level] || level
    },
    getTimelineType(status) {
      const typeMap = {
        'APPROVED': 'success',
        'REJECTED': 'danger',
        'PENDING': 'warning',
        'TRANSFERRED': 'warning',
        'WITHDRAWN': 'info',
        'SKIPPED': 'info'
      }
      return typeMap[status] || 'info'
    },
    getTimelineColor(status) {
      const colorMap = {
        'APPROVED': '#67c23a',
        'REJECTED': '#f56c6c',
        'PENDING': '#e6a23c',
        'TRANSFERRED': '#409eff',
        'WITHDRAWN': '#909399',
        'SKIPPED': '#c0c4cc'
      }
      return colorMap[status] || '#409eff'
    },
    getTimelineIcon(status) {
      const iconMap = {
        'APPROVED': 'el-icon-circle-check',
        'REJECTED': 'el-icon-circle-close',
        'PENDING': 'el-icon-time',
        'TRANSFERRED': 'el-icon-right',
        'WITHDRAWN': 'el-icon-refresh-left',
        'SKIPPED': 'el-icon-minus'
      }
      return iconMap[status] || 'el-icon-more'
    },
    isAutoUpgrade(node) {
      return node.transferredReason && node.transferredReason.includes('超时')
    }
  }
}
</script>

<style scoped>
.approval-overview {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 10px;
}

.level-item {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  border-radius: 8px;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.level-item.pending {
  border: 2px solid #e6a23c;
}

.level-item.approved {
  border: 2px solid #67c23a;
}

.level-item.rejected {
  border: 2px solid #f56c6c;
}

.level-item.transferred {
  border: 2px solid #409eff;
}

.level-item.skipped {
  border: 2px solid #c0c4cc;
  opacity: 0.6;
}

.level-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  font-size: 18px;
}

.level-item.pending .level-icon {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.level-item.approved .level-icon {
  background-color: #f0f9eb;
  color: #67c23a;
}

.level-item.rejected .level-icon {
  background-color: #fef0f0;
  color: #f56c6c;
}

.level-item.transferred .level-icon {
  background-color: #ecf5ff;
  color: #409eff;
}

.level-item.skipped .level-icon {
  background-color: #f4f4f5;
  color: #c0c4cc;
}

.level-info {
  text-align: left;
}

.level-name {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.level-approver {
  font-size: 12px;
  color: #606266;
  margin-top: 3px;
}

.level-arrow {
  margin: 0 15px;
  color: #c0c4cc;
  font-size: 20px;
}

.timeline-card {
  margin-bottom: 10px;
}

.timeline-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.timeline-level {
  font-weight: bold;
  color: #303133;
}

.timeline-approver {
  color: #409eff;
}

.timeline-comment,
.timeline-transfer,
.timeline-transfer-detail,
.timeline-time {
  padding: 5px 0;
  font-size: 13px;
}

.timeline-comment {
  color: #606266;
}

.timeline-transfer {
  color: #e6a23c;
}

.timeline-transfer-detail {
  color: #909399;
}

.timeline-time {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.el-timeline {
  padding-left: 10px;
}
</style>
