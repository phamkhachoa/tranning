# News API — Mẫu request

Base URL: `http://localhost:8080`

Mọi response đều bọc trong `BaseResponse`:

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

Khi lỗi, `data` vắng mặt:

```json
{
  "code": 404,
  "message": "News 99 is not found"
}
```

---

## GET /api/news — Danh sách tin (phân trang)

**Request**
```http
GET /api/news?pageNo=0&pageSize=10
```

```bash
curl -X GET "http://localhost:8080/api/news?pageNo=0&pageSize=10"
```

**Response 200**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "newsContent": [
      {
        "id": 2,
        "title": "Tin tức đầu tiên",
        "slug": "tin-tuc-dau-tien",
        "summary": "Tóm tắt ngắn",
        "author": "hoapham",
        "thumbnailUrl": null,
        "status": "PUBLISHED",
        "createdOn": "2026-06-10T04:50:17.662853Z"
      }
    ],
    "pageNo": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "isLast": true
  }
}
```

---

## GET /api/news/{id} — Chi tiết tin

**Request**
```http
GET /api/news/1
```

```bash
curl -X GET "http://localhost:8080/api/news/1"
```

**Response 200**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "title": "Tin tức đầu tiên",
    "slug": "tin-tuc-dau-tien",
    "summary": "Tóm tắt ngắn",
    "content": "Nội dung chi tiết...",
    "author": "hoapham",
    "thumbnailUrl": null,
    "status": "PUBLISHED",
    "createdOn": "2026-06-10T04:50:17.662853Z",
    "lastModifiedOn": "2026-06-10T04:50:17.662853Z"
  }
}
```

**Response 404**
```json
{
  "code": 404,
  "message": "News 99 is not found"
}
```

---

## POST /api/news — Tạo tin mới

**Request**
```http
POST /api/news
Content-Type: application/json
```

```bash
curl -X POST "http://localhost:8080/api/news" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tiêu đề bài viết",
    "slug": "tieu-de-bai-viet",
    "summary": "Tóm tắt bài viết không quá 1000 ký tự",
    "content": "Nội dung chi tiết của bài viết...",
    "author": "hoapham",
    "thumbnailUrl": "https://example.com/images/thumbnail.jpg",
    "status": "PUBLISHED"
  }'
```

> `slug` không bắt buộc — nếu bỏ qua, slug sẽ tự sinh từ `title` (có hỗ trợ tiếng Việt).
>
> `status` nhận một trong ba giá trị: `DRAFT` | `PUBLISHED` | `ARCHIVED`

**Response 201**
```json
{
  "code": 201,
  "message": "Success",
  "data": {
    "id": 5,
    "title": "Tiêu đề bài viết",
    "slug": "tieu-de-bai-viet",
    "summary": "Tóm tắt bài viết không quá 1000 ký tự",
    "content": "Nội dung chi tiết của bài viết...",
    "author": "hoapham",
    "thumbnailUrl": "https://example.com/images/thumbnail.jpg",
    "status": "PUBLISHED",
    "createdOn": "2026-06-10T04:55:00.000000Z",
    "lastModifiedOn": "2026-06-10T04:55:00.000000Z"
  }
}
```

**Response 400 — Validation**
```json
{
  "code": 400,
  "message": "Request information is not valid",
  "errors": [
    "title must not be blank",
    "status must not be null"
  ]
}
```

**Response 400 — Slug trùng**
```json
{
  "code": 400,
  "message": "Slug tieu-de-bai-viet is already existed"
}
```

---

## PUT /api/news/{id} — Cập nhật tin

**Request**
```http
PUT /api/news/5
Content-Type: application/json
```

```bash
curl -X PUT "http://localhost:8080/api/news/5" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Tiêu đề đã chỉnh sửa",
    "summary": "Tóm tắt mới",
    "content": "Nội dung mới...",
    "author": "hoapham",
    "thumbnailUrl": "https://example.com/images/new-thumbnail.jpg",
    "status": "DRAFT"
  }'
```

**Response 200** — trả về tin sau khi cập nhật
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 5,
    "title": "Tiêu đề đã chỉnh sửa",
    "slug": "tieu-de-da-chinh-sua",
    "summary": "Tóm tắt mới",
    "content": "Nội dung mới...",
    "author": "hoapham",
    "thumbnailUrl": "https://example.com/images/new-thumbnail.jpg",
    "status": "DRAFT",
    "createdOn": "2026-06-10T04:55:00.000000Z",
    "lastModifiedOn": "2026-06-10T05:10:00.000000Z"
  }
}
```

**Response 404**
```json
{
  "code": 404,
  "message": "News 99 is not found"
}
```

---

## DELETE /api/news/{id} — Xoá tin

**Request**
```http
DELETE /api/news/5
```

```bash
curl -X DELETE "http://localhost:8080/api/news/5"
```

**Response 200**
```json
{
  "code": 200,
  "message": "Success"
}
```

**Response 404**
```json
{
  "code": 404,
  "message": "News 99 is not found"
}
```

---

## Status values

| Giá trị     | Mô tả        |
|-------------|--------------|
| `DRAFT`     | Bản nháp     |
| `PUBLISHED` | Đã xuất bản  |
| `ARCHIVED`  | Đã lưu trữ   |

## Swagger UI

Truy cập `http://localhost:8080/swagger-ui.html` để thử trực tiếp trên giao diện.
