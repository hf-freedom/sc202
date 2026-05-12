import Vue from 'vue'
import VueRouter from 'vue-router'
import MyReimbursement from '../views/MyReimbursement.vue'
import CreateReimbursement from '../views/CreateReimbursement.vue'
import ApprovalList from '../views/ApprovalList.vue'
import BudgetView from '../views/BudgetView.vue'
import ReimbursementDetail from '../views/ReimbursementDetail.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'MyReimbursement',
    component: MyReimbursement
  },
  {
    path: '/create',
    name: 'CreateReimbursement',
    component: CreateReimbursement
  },
  {
    path: '/edit/:id',
    name: 'EditReimbursement',
    component: CreateReimbursement
  },
  {
    path: '/resubmit/:id',
    name: 'ResubmitReimbursement',
    component: CreateReimbursement
  },
  {
    path: '/approval',
    name: 'ApprovalList',
    component: ApprovalList
  },
  {
    path: '/budget',
    name: 'BudgetView',
    component: BudgetView
  },
  {
    path: '/detail/:id',
    name: 'ReimbursementDetail',
    component: ReimbursementDetail
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
