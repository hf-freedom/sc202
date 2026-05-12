<template>
  <div class="create-reimbursement">
    <el-card>
      <div slot="header">
        <span>{{ pageTitle }}</span>
      </div>
      <el-form :model="form" :rules="rules" ref="form" label-width="100px" style="max-width: 800px;">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入报销标题"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" placeholder="请输入报销描述" rows="4"></el-input>
        </el-form-item>
        <el-form-item label="金额(元)" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" placeholder="请输入金额"></el-input-number>
          <el-alert v-if="budgetInfo" :title="budgetInfoText" :type="budgetInfo ? 'info' : 'error'" :closable="false" style="margin-top: 10px;"></el-alert>
        </el-form-item>
        <el-form-item label="发票号" prop="invoiceNumber">
          <el-input v-model="form.invoiceNumber" placeholder="请输入发票号" @blur="checkInvoice"></el-input>
          <el-alert v-if="invoiceCheckResult !== null" :title="invoiceCheckText" :type="invoiceCheckResult ? 'success' : 'error'" :closable="false" style="margin-top: 10px;"></el-alert>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" v-if="!isEditMode">提交</el-button>
          <el-button @click="handleSaveDraft">保存草稿</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'CreateReimbursement',
  data() {
    return {
      form: {
        title: '',
        description: '',
        amount: null,
        invoiceNumber: ''
      },
      rules: {
        title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
        amount: [{ required: true, message: '请输入金额', trigger: 'blur' }]
      },
      budgetInfo: null,
      invoiceCheckResult: null,
      editId: null,
      resubmitId: null,
      originalReimburs: null
    }
  },
  computed: {
    pageTitle() {
      if (this.resubmitId) return '重提报销'
      if (this.editId) return '编辑报销'
      return '新建报销'
    },
    isEditMode() {
      return !!this.editId
    },
    budgetInfoText() {
      if (!this.budgetInfo) return ''
      return `部门预算：总额 ${this.budgetInfo.totalBudget}，已用 ${this.budgetInfo.usedBudget}，已占用 ${this.budgetInfo.reservedBudget}，可用 ${this.budgetInfo.availableBudget}`
    },
    invoiceCheckText() {
      if (this.invoiceCheckResult === null) return ''
      if (this.invoiceCheckResult) return '发票号可用'
      return '该发票号已被使用，无法重复报销'
    }
  },
  created() {
    this.editId = this.$route.params.id && this.$route.name === 'EditReimbursement' ? this.$route.params.id : null
    this.resubmitId = this.$route.params.id && this.$route.name === 'ResubmitReimbursement' ? this.$route.params.id : null
    
    if (this.editId) {
      this.loadReimbursement(this.editId)
    }
    if (this.resubmitId) {
      this.loadReimbursement(this.resubmitId)
    }
  },
  methods: {
    getCurrentUserId() {
      return localStorage.getItem('currentUserId') || 'user_emp1'
    },
    async loadReimbursement(id) {
      try {
        const res = await this.$axios.get(`/api/reimbursements/${id}`)
        if (res.data.success) {
          const data = res.data.data
          this.originalReimburs = data
          this.form = {
            title: data.title,
            description: data.description,
            amount: data.amount,
            invoiceNumber: data.invoiceNumber
          }
        }
      } catch (e) {
        this.$message.error('加载失败')
      }
    },
    async checkBudget() {
      if (!this.form.amount) return
      try {
        const userId = this.getCurrentUserId()
        const userRes = await this.$axios.get(`/api/users/${userId}`)
        if (userRes.data.success) {
          const user = userRes.data.data
          const budgetRes = await this.$axios.get(`/api/budgets/${user.departmentId}`)
          if (budgetRes.data.success) {
            this.budgetInfo = budgetRes.data.data
          }
        }
      } catch (e) {
        console.error('检查预算失败', e)
      }
    },
    async checkInvoice() {
      if (!this.form.invoiceNumber) {
        this.invoiceCheckResult = null
        return
      }
      try {
        const res = await this.$axios.get(`/api/invoices/check/${this.form.invoiceNumber}`)
        if (res.data.success) {
          const data = res.data.data
          this.invoiceCheckResult = !data.used
        }
      } catch (e) {
        console.error('检查发票失败', e)
      }
    },
    async handleSubmit() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          if (this.resubmitId) {
            await this.doResubmit()
          } else {
            await this.doSave(true)
          }
        }
      })
    },
    async handleSaveDraft() {
      await this.doSave(false)
    },
    async doSave(isSubmit) {
      try {
        const userId = this.getCurrentUserId()
        let reimbursId
        
        if (this.editId) {
          const updateRes = await this.$axios.put(`/api/reimbursements/draft/${this.editId}`, this.form)
          if (!updateRes.data.success) {
            this.$message.error(updateRes.data.message || '保存失败')
            return
          }
          reimbursId = this.editId
        } else {
          const saveRes = await this.$axios.post('/api/reimbursements/draft', this.form, {
            params: { applicantId: userId }
          })
          if (!saveRes.data.success) {
            this.$message.error(saveRes.data.message || '保存失败')
            return
          }
          reimbursId = saveRes.data.data.id
        }

        if (isSubmit) {
          const submitRes = await this.$axios.post(`/api/reimbursements/${reimbursId}/submit`)
          if (submitRes.data.success) {
            this.$message.success('提交成功')
            this.$router.push('/')
          } else {
            this.$message.error(submitRes.data.message || '提交失败')
          }
        } else {
          this.$message.success('草稿保存成功')
          this.$router.push('/')
        }
      } catch (e) {
        this.$message.error('操作失败')
      }
    },
    async doResubmit() {
      try {
        const res = await this.$axios.post(`/api/reimbursements/${this.resubmitId}/resubmit`, this.form)
        if (res.data.success) {
          this.$message.success('重提成功')
          this.$router.push('/')
        } else {
          this.$message.error(res.data.message || '重提失败')
        }
      } catch (e) {
        this.$message.error('重提失败')
      }
    },
    goBack() {
      this.$router.go(-1)
    }
  },
  watch: {
    'form.amount': 'checkBudget'
  }
}
</script>
