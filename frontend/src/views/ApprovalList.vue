<template>
  <div class="approval-list">
    <el-card>
      <div slot="header" class="card-header">
        <span>待我审批</span>
        <div class="header-actions">
          <el-button type="primary" size="small" @click="refreshTimeoutStatus">
            <i class="el-icon-refresh"></i> 刷新超时状态
          </el-button>
          <el-button type="warning" size="small" @click="triggerTimeoutCheck">
            <i class="el-icon-time"></i> 手动检查超时
          </el-button>
          <el-button type="info" size="small" @click="showHistoryDialog">
            <i class="el-icon-document"></i> 升级记录
          </el-button>
        </div>
      </div>
      
      <el-alert
        v-if="timeoutStatus.pendingCount > 0"
        :title="`您有 ${timeoutStatus.pendingCount} 条待审批记录，超时时间 ${timeoutStatus.timeoutThreshold} 分钟`"
        type="warning"
        :closable="false"
        style="margin-bottom: 15px;"
        show-icon
      ></el-alert>

      <el-table :data="approvalList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="报销单号" width="260"></el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="90"></el-table-column>
        <el-table-column prop="title" label="标题" width="150"></el-table-column>
        <el-table-column prop="amount" label="金额(元)" width="120">
          <template slot-scope="scope">
            <span style="color: #409EFF; font-weight: bold">{{ scope.row.amount }}</span>
            <el-tag v-if="Number(scope.row.amount) < 5000" type="success" size="mini" style="margin-left: 5px">小额</el-tag>
            <el-tag v-else type="warning" size="mini" style="margin-left: 5px">大额</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审批超时状态" width="150">
          <template slot-scope="scope">
            <div v-if="getTimeoutInfo(scope.row.id)">
              <el-progress
                :percentage="getTimeoutInfo(scope.row.id).progressPercent"
                :status="getTimeoutInfo(scope.row.id).isTimeout ? 'exception' : getTimeoutInfo(scope.row.id).progressPercent > 80 ? 'warning' : ''"
                :show-text="false"
                style="margin-bottom: 5px;"
              ></el-progress>
              <div class="timeout-text">
                <span v-if="getTimeoutInfo(scope.row.id).isTimeout" class="timeout-danger">已超时 {{ getTimeoutInfo(scope.row.id).minutesElapsed }} 分钟</span>
                <span v-else class="timeout-normal">剩余 {{ getTimeoutInfo(scope.row.id).minutesRemaining }} 分钟</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审批链" width="280">
          <template slot-scope="scope">
            <div v-if="scope.row.approvalTrail && scope.row.approvalTrail.length > 0" class="approval-chain">
              <span
                v-for="(node, index) in sortedNodes(scope.row.approvalTrail)"
                :key="node.id"
                class="chain-node"
                :class="getApprovalNodeClass(node.status)"
                :title="`${getApprovalLevelShortText(node.approvalLevel)} - ${node.approverName}`"
              >
                <i :class="getApprovalNodeIcon(node.status)"></i>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" width="160"></el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="viewDetail(scope.row.id)">详情</el-button>
            <el-button type="text" size="small" @click="handleApprove(scope.row)">通过</el-button>
            <el-button type="text" size="small" @click="handleReject(scope.row)">驳回</el-button>
            <el-button type="text" size="small" @click="handleTransfer(scope.row)">转交</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="审批通过" :visible.sync="approveDialogVisible" width="500px">
      <div class="dialog-info">
        <p><strong>报销单号：</strong>{{ currentReimburs && currentReimburs.id }}</p>
        <p><strong>申请人：</strong>{{ currentReimburs && currentReimburs.applicantName }}</p>
        <p><strong>金额：</strong>{{ currentReimburs && currentReimburs.amount }} 元</p>
      </div>
      <el-input type="textarea" v-model="approveComment" placeholder="请输入审批意见(可选)" rows="3"></el-input>
      <span slot="footer" class="dialog-footer">
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApprove">确认通过</el-button>
      </span>
    </el-dialog>

    <el-dialog title="驳回申请" :visible.sync="rejectDialogVisible" width="500px">
      <div class="dialog-info">
        <p><strong>报销单号：</strong>{{ currentReimburs && currentReimburs.id }}</p>
        <p><strong>申请人：</strong>{{ currentReimburs && currentReimburs.applicantName }}</p>
        <p><strong>金额：</strong>{{ currentReimburs && currentReimburs.amount }} 元</p>
      </div>
      <el-input type="textarea" v-model="rejectComment" placeholder="请输入驳回原因" rows="3"></el-input>
      <span slot="footer" class="dialog-footer">
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认驳回</el-button>
      </span>
    </el-dialog>

    <el-dialog title="转交审批" :visible.sync="transferDialogVisible" width="500px">
      <el-form label-width="80px">
        <el-form-item label="转交人">
          <el-select v-model="transferToUserId" placeholder="请选择转交人" style="width: 100%;">
            <el-option v-for="user in availableUsers" :key="user.id" :label="user.name" :value="user.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input type="textarea" v-model="transferReason" placeholder="请输入转交原因" rows="3"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmTransfer">确认转交</el-button>
      </span>
    </el-dialog>

    <el-dialog title="超时升级记录" :visible.sync="historyDialogVisible" width="700px">
      <el-table :data="timeoutHistory" border style="width: 100%" v-loading="historyLoading">
        <el-table-column prop="reimbursementTitle" label="报销标题" width="180"></el-table-column>
        <el-table-column prop="fromApprover" label="原审批人" width="100"></el-table-column>
        <el-table-column prop="toApprover" label="升级至" width="100"></el-table-column>
        <el-table-column prop="timeoutMinutes" label="超时时长" width="100">
          <template slot-scope="scope">{{ scope.row.timeoutMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column label="触发方式" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isScheduled ? 'info' : 'warning'" size="mini">
              {{ scope.row.isScheduled ? '自动检测' : '手动触发' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="processedAt" label="处理时间" width="180"></el-table-column>
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="historyDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ApprovalList',
  data() {
    return {
      approvalList: [],
      loading: false,
      approveDialogVisible: false,
      rejectDialogVisible: false,
      transferDialogVisible: false,
      historyDialogVisible: false,
      historyLoading: false,
      currentReimburs: null,
      approveComment: '',
      rejectComment: '',
      transferToUserId: '',
      transferReason: '',
      availableUsers: [],
      timeoutStatus: {
        pendingCount: 0,
        timeoutThreshold: 1,
        pendingList: []
      },
      timeoutHistory: []
    }
  },
  created() {
    this.loadApprovals()
    this.loadUsers()
    this.loadTimeoutStatus()
  },
  methods: {
    getCurrentUserId() {
      return localStorage.getItem('currentUserId') || 'user_emp1'
    },
    async loadApprovals() {
      this.loading = true
      try {
        const userId = this.getCurrentUserId()
        const res = await this.$axios.get(`/api/reimbursements/approver/${userId}`)
        if (res.data.success) {
          this.approvalList = res.data.data
        }
      } catch (e) {
        this.$message.error('加载失败')
      } finally {
        this.loading = false
      }
    },
    async loadTimeoutStatus() {
      try {
        const userId = this.getCurrentUserId()
        const res = await this.$axios.get(`/api/timeout/status/${userId}`)
        if (res.data.success) {
          this.timeoutStatus = res.data.data
        }
      } catch (e) {
        console.error('加载超时状态失败', e)
      }
    },
    async refreshTimeoutStatus() {
      await this.loadTimeoutStatus()
      await this.loadApprovals()
      this.$message.success('状态已刷新')
    },
    async triggerTimeoutCheck() {
      this.$confirm('确定要手动执行超时检查吗？达到超时时间的审批将自动升级给上级审批人。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$axios.post('/api/timeout/trigger')
          if (res.data.success) {
            const processedCount = res.data.data.processedCount
            if (processedCount > 0) {
              this.$message.success(`检查完成，已自动升级 ${processedCount} 条超时审批`)
            } else {
              this.$message.info('检查完成，没有需要升级的超时审批')
            }
            await this.loadApprovals()
            await this.loadTimeoutStatus()
          } else {
            this.$message.error(res.data.message || '检查失败')
          }
        } catch (e) {
          this.$message.error('检查失败')
        }
      }).catch(() => {})
    },
    async showHistoryDialog() {
      this.historyDialogVisible = true
      this.historyLoading = true
      try {
        const res = await this.$axios.get('/api/timeout/history')
        if (res.data.success) {
          this.timeoutHistory = res.data.data
        }
      } catch (e) {
        this.$message.error('加载历史记录失败')
      } finally {
        this.historyLoading = false
      }
    },
    async loadUsers() {
      try {
        const res = await this.$axios.get('/api/users')
        if (res.data.success) {
          this.availableUsers = res.data.data
        }
      } catch (e) {
        console.error('加载用户失败', e)
      }
    },
    sortedNodes(nodes) {
      return [...nodes].sort((a, b) => a.nodeOrder - b.nodeOrder)
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
    getTimeoutInfo(reimbursementId) {
      return this.timeoutStatus.pendingList.find(item => item.reimbursementId === reimbursementId)
    },
    viewDetail(id) {
      this.$router.push(`/detail/${id}`)
    },
    handleApprove(row) {
      this.currentReimburs = row
      this.approveComment = ''
      this.approveDialogVisible = true
    },
    handleReject(row) {
      this.currentReimburs = row
      this.rejectComment = ''
      this.rejectDialogVisible = true
    },
    handleTransfer(row) {
      this.currentReimburs = row
      this.transferToUserId = ''
      this.transferReason = ''
      this.transferDialogVisible = true
    },
    async confirmApprove() {
      try {
        const userId = this.getCurrentUserId()
        const res = await this.$axios.post(`/api/reimbursements/${this.currentReimburs.id}/approve`, null, {
          params: { approverId: userId, comment: this.approveComment }
        })
        if (res.data.success) {
          this.$message.success('审批通过')
          this.approveDialogVisible = false
          this.loadApprovals()
          this.loadTimeoutStatus()
        } else {
          this.$message.error(res.data.message || '审批失败')
        }
      } catch (e) {
        this.$message.error('审批失败')
      }
    },
    async confirmReject() {
      if (!this.rejectComment.trim()) {
        this.$message.error('请输入驳回原因')
        return
      }
      try {
        const userId = this.getCurrentUserId()
        const res = await this.$axios.post(`/api/reimbursements/${this.currentReimburs.id}/reject`, null, {
          params: { approverId: userId, comment: this.rejectComment }
        })
        if (res.data.success) {
          this.$message.success('已驳回')
          this.rejectDialogVisible = false
          this.loadApprovals()
          this.loadTimeoutStatus()
        } else {
          this.$message.error(res.data.message || '驳回失败')
        }
      } catch (e) {
        this.$message.error('驳回失败')
      }
    },
    async confirmTransfer() {
      if (!this.transferToUserId) {
        this.$message.error('请选择转交人')
        return
      }
      try {
        const userId = this.getCurrentUserId()
        const res = await this.$axios.post(`/api/reimbursements/${this.currentReimburs.id}/transfer`, null, {
          params: { 
            fromApproverId: userId, 
            toApproverId: this.transferToUserId,
            reason: this.transferReason 
          }
        })
        if (res.data.success) {
          this.$message.success('转交成功')
          this.transferDialogVisible = false
          this.loadApprovals()
          this.loadTimeoutStatus()
        } else {
          this.$message.error(res.data.message || '转交失败')
        }
      } catch (e) {
        this.$message.error('转交失败')
      }
    }
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.dialog-info {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.dialog-info p {
  margin: 5px 0;
  color: #606266;
}

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
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 8px;
  font-size: 14px;
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

.timeout-text {
  font-size: 12px;
  text-align: center;
}

.timeout-normal {
  color: #67c23a;
}

.timeout-danger {
  color: #f56c6c;
  font-weight: bold;
}
</style>
