import request from '@/utils/request'

function postFile(url, file, onUploadProgress) {
  const formData = new FormData()
  formData.append('file', file)

  return request({
    url,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress
  })
}

export function uploadFile(file, onUploadProgress) {
  return postFile('/upload', file, onUploadProgress)
}

export function uploadEquipmentImage(file, onUploadProgress) {
  return postFile('/upload/equipment-image', file, onUploadProgress)
}
