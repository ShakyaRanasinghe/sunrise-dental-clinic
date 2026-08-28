<%-- 404. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Page not found — Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${ctx}/css/app.css">
</head>
<body>
<div class="narrow">
    <div class="card">
        <h1 class="page-title">Page not found</h1>
        <p class="page-subtitle">That address does not exist in the clinic system.</p>
        <div class="form-actions">
            <a class="btn" href="${ctx}/">Back to the start</a>
        </div>
    </div>
</div>
</body>
</html>
