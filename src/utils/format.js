// 日期格式化
export const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

// 金额格式化
export const formatMoney = (money) => {
    if (!money) return '0.00'
    return parseFloat(money).toFixed(2)
}

// 状态标签类型映射
export const statusTypeMap = {
    '待提交': 'info',
    '已提交': 'warning',
    '审核驳回': 'danger',
    '待二次审核': 'warning',
    '待三次审核': 'warning',
    '审核通过': 'success',
    '已报销': 'success'
}