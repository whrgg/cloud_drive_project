<template>
  <div class="share-manage-container">
    <div class="page-header">
      <h2>我的分享</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索分享"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button @click="handleSearch">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>

        <el-select v-model="filterStatus" placeholder="状态筛选" @change="handleFilterChange">
          <el-option label="全部" :value="-1" />
          <el-option label="有效" :value="0" />
          <el-option label="已取消" :value="1" />
          <el-option label="已过期" :value="2" />
        </el-select>

        <el-button @click="refreshList">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 分享列表 -->
    <div class="share-list-wrapper">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 没有数据 -->
      <el-empty v-else-if="shareList.length === 0" description="暂无分享记录">
        <el-button type="primary" @click="goToHome">返回文件列表</el-button>
      </el-empty>

      <!-- 分享列表 -->
      <div v-else class="share-list">
        <el-table
          :data="shareList"
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />

          <el-table-column prop="fileName" label="文件名" min-width="180">
            <template #default="{ row }">
              <div class="file-item">
                <el-icon v-if="row.isDir"><Folder /></el-icon>
                <el-icon v-else-if="row.fileType === 'image'"><Picture /></el-icon>
                <el-icon v-else-if="row.fileType === 'video'"><VideoPlay /></el-icon>
                <el-icon v-else-if="row.fileType === 'audio'"><Headset /></el-icon>
                <el-icon v-else><Document /></el-icon>
                <span class="file-name">{{ row.fileName }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="分享链接" min-width="220">
            <template #default="{ row }">
              <div class="share-link">
                <el-input :value="getShareUrl(row.shareCode || row.id)" readonly>
                  <template #append>
                    <el-tooltip content="复制链接">
                      <el-button @click="copyShareUrl(row.shareCode || row.id)">
                        <el-icon><DocumentCopy /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </template>
                </el-input>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="code" label="提取码" width="100">
            <template #default="{ row }">
              <span v-if="row.code">{{ row.code }}</span>
              <span v-else class="no-code">无</span>
            </template>
          </el-table-column>

          <el-table-column prop="views" label="浏览/下载" width="120">
            <template #default="{ row }">
              <span>{{ row.viewCount || row.views || 0 }}/{{ row.downloadCount || row.downloads || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="expireTime" label="有效期" width="180">
            <template #default="{ row }">
              <span v-if="row.expireTime">{{ formatDate(row.expireTime) }}</span>
              <span v-else class="no-expire">永久有效</span>
            </template>
          </el-table-column>

          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag
                :type="getShareStatusType(row.status)"
                size="small"
              >
                {{ getShareStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button
                  size="small"
                  type="primary"
                  text
                  @click="openShare(row.shareCode || row.id)"
                  :disabled="row.status !== 0"
                >
                  <el-icon><View /></el-icon>
                </el-button>

                <el-button
                  size="small"
                  type="primary"
                  text
                  @click="handleShareAction(row.shareCode || row.id, 'cancel')"
                  :disabled="row.status !== 0"
                >
                  <el-icon><Close /></el-icon>
                </el-button>

                <el-dropdown trigger="click" @command="(command) => handleShareAction(row.shareCode || row.id, command)">
                  <el-button size="small" type="primary" text>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="qrcode">显示二维码</el-dropdown-item>
                      <el-dropdown-item command="detail">查看详情</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除记录</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 批量操作 -->
    <div class="batch-actions" v-if="selectedShares.length > 0">
      <span class="selected-count">已选择 {{ selectedShares.length }} 项</span>
      <el-button-group>
        <el-button @click="handleBatchCancel">取消分享</el-button>
        <el-button type="danger" @click="handleBatchDelete">删除记录</el-button>
      </el-button-group>
    </div>

    <!-- 二维码对话框 -->
    <el-dialog v-model="qrcodeDialogVisible" title="分享二维码" width="300px" align-center>
      <div class="qrcode-container">
        <div v-if="currentQrCodeUrl" class="qrcode-image">
          <img :src="currentQrCodeUrl" alt="分享二维码" />
        </div>
        <div class="share-info text-center">
          <p>使用手机扫描二维码查看分享</p>
        </div>
      </div>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="分享详情" width="500px">
      <div v-if="currentShareDetail" class="share-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="文件名">{{ currentShareDetail.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ currentShareDetail.isDir ? '文件夹' : currentShareDetail.fileType }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ currentShareDetail.isDir ? '-' : formatSize(currentShareDetail.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="分享ID">{{ currentShareDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="提取码">{{ currentShareDetail.code || currentShareDetail.extractionCode || '无' }}</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ currentShareDetail.expireTime ? formatDate(currentShareDetail.expireTime) : '永久有效' }}</el-descriptions-item>
          <el-descriptions-item label="浏览次数">{{ currentShareDetail.viewCount || currentShareDetail.views || 0 }}</el-descriptions-item>
          <el-descriptions-item label="下载次数">{{ currentShareDetail.downloadCount || currentShareDetail.downloads || 0 }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(currentShareDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ getShareStatusText(currentShareDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="分享说明">{{ currentShareDetail.description || '无' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onActivated, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getUserShares, cancelShare, batchCancelShare } from '../../api/share';
import type { ShareInfo } from '../../types/share';
import QRCode from 'qrcode';

const router = useRouter();

// 状态
const loading = ref(false);
const searchKeyword = ref('');
const filterStatus = ref(-1); // -1表示全部
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);
const shareList = ref<ShareInfo[]>([]);
const selectedShares = ref<ShareInfo[]>([]);
const qrcodeDialogVisible = ref(false);
const currentQrCodeUrl = ref('');
const detailDialogVisible = ref(false);
const currentShareDetail = ref<ShareInfo | null>(null);

// 加载分享列表
const loadShareList = async () => {
  loading.value = true;
  try {
    // TODO: 调整API实际参数
    const res = await getUserShares({
      page: currentPage.value,
      size: pageSize.value,
      // 其他筛选条件
    });
    
    console.log('获取到的分享列表数据:', res);
    
    shareList.value = res || [];
    total.value = res.length || 0;
  } catch (error) {
    console.error('Failed to load share list', error);
    ElMessage.error('加载分享列表失败');
  } finally {
    loading.value = false;
  }
};

// 刷新列表
const refreshList = () => {
  loadShareList();
};

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1; // 搜索时重置页码
  loadShareList();
};

// 处理筛选器变化
const handleFilterChange = () => {
  currentPage.value = 1; // 筛选时重置页码
  loadShareList();
};

// 处理页码大小变化
const handleSizeChange = (val: number) => {
  pageSize.value = val;
  loadShareList();
};

// 处理当前页变化
const handleCurrentChange = (val: number) => {
  currentPage.value = val;
  loadShareList();
};

// 处理多选变化
const handleSelectionChange = (val: ShareInfo[]) => {
  selectedShares.value = val;
};

// 获取完整分享URL
const getShareUrl = (shareId: string) => {
  return `${window.location.origin}/s/${shareId}`;
};

// 复制分享链接
const copyShareUrl = (shareId: string) => {
  const url = getShareUrl(shareId);
  
  navigator.clipboard.writeText(url)
    .then(() => {
      ElMessage.success('分享链接已复制');
    })
    .catch((err) => {
      console.error('复制失败:', err);
      ElMessage.error('复制失败，请手动复制');
    });
};

// 打开分享
const openShare = (shareId: string) => {
  window.open(`/s/${shareId}`, '_blank');
};

// 生成二维码
const generateQrCode = async (shareId: string) => {
  const url = getShareUrl(shareId);
  
  try {
    // 生成二维码图片URL
    const qrUrl = await QRCode.toDataURL(url, {
      width: 200,
      margin: 1,
      color: {
        dark: '#000000',
        light: '#ffffff'
      }
    });
    
    currentQrCodeUrl.value = qrUrl;
    qrcodeDialogVisible.value = true;
  } catch (error) {
    console.error('生成二维码失败:', error);
    ElMessage.error('生成二维码失败');
  }
};

// 获取分享详情
const getShareDetail = async (shareId: string) => {
  // 尝试通过id或shareCode查找分享
  const share = shareList.value.find(s => s.id === shareId || s.shareCode === shareId);
  
  if (share) {
    console.log('分享详情:', share);
    currentShareDetail.value = share;
    detailDialogVisible.value = true;
  } else {
    ElMessage.error('未找到分享详情');
  }
};

// 取消分享
const cancelShareItem = async (shareId: string) => {
  try {
    console.log('取消分享，ID:', shareId);
    await cancelShare(shareId);
    ElMessage.success('已取消分享');
    loadShareList(); // 重新加载列表
  } catch (error) {
    console.error('Failed to cancel share', error);
    ElMessage.error('取消分享失败');
  }
};

// 删除分享记录
const deleteShareItem = async (shareId: string) => {
  // 此功能可能需要后端支持
  ElMessage.info('删除分享记录功能即将上线');
  loadShareList(); // 重新加载列表
};

// 处理分享操作
const handleShareAction = (shareId: string, action: string) => {
  switch (action) {
    case 'cancel':
      ElMessageBox.confirm('确定要取消该分享吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        cancelShareItem(shareId);
      });
      break;
    case 'qrcode':
      generateQrCode(shareId);
      break;
    case 'detail':
      getShareDetail(shareId);
      break;
    case 'delete':
      ElMessageBox.confirm('确定要删除该分享记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteShareItem(shareId);
      });
      break;
    default:
      break;
  }
};

// 批量取消分享
const handleBatchCancel = () => {
  if (selectedShares.value.length === 0) {
    ElMessage.warning('请先选择要操作的分享');
    return;
  }
  
  ElMessageBox.confirm(`确定要取消选中的 ${selectedShares.value.length} 个分享吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const shareIds = selectedShares.value.map(share => share.shareCode || share.id);
      await batchCancelShare(shareIds);
      ElMessage.success('批量取消分享成功');
      loadShareList(); // 重新加载列表
    } catch (error) {
      console.error('Failed to batch cancel shares', error);
      ElMessage.error('批量取消分享失败');
    }
  });
};

// 批量删除分享记录
const handleBatchDelete = () => {
  if (selectedShares.value.length === 0) {
    ElMessage.warning('请先选择要操作的分享');
    return;
  }
  
  // 此功能可能需要后端支持
  ElMessage.info('批量删除分享记录功能即将上线');
};

// 返回文件列表
const goToHome = () => {
  router.push('/');
};

// 获取分享状态文本
const getShareStatusText = (status: number) => {
  switch (status) {
    case 0:
      return '已取消';
    case 1:
      return '有效';
    case 2:
      return '已过期';
    default:
      return '未知';
  }
};

// 获取分享状态标签类型
const getShareStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'success';
    case 1:
      return 'info';
    case 2:
      return 'warning';
    default:
      return 'info';
  }
};

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 格式化文件大小
const formatSize = (bytes: number) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i];
};

// 初始化
onMounted(() => {
  console.log('ShareManage组件加载，开始获取分享列表');
  loadShareList();
});

// 当组件从缓存激活时也刷新数据
onActivated(() => {
  console.log('ShareManage组件激活，刷新分享列表');
  loadShareList();
});
</script>

<style scoped>
.share-manage-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.share-list-wrapper {
  flex: 1;
  overflow: auto;
}

.loading-container {
  padding: 20px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-code, .no-expire {
  color: #909399;
}

.pagination-container {
  padding: 20px 0;
  display: flex;
  justify-content: flex-end;
}

.batch-actions {
  padding: 10px;
  background-color: #f5f7fa;
  border-top: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.qrcode-image {
  border: 1px solid #e0e0e0;
  padding: 10px;
  background-color: #fff;
}

.qrcode-image img {
  max-width: 200px;
  max-height: 200px;
}

.text-center {
  text-align: center;
}
</style> 