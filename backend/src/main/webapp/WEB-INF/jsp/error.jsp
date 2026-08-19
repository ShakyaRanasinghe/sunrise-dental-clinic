<%-- Shown when a page cannot be completed. Never displays internal detail. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Something went wrong — Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${ctx}/css/app.css">
</head>
<body>
<div class="narrow">
    <div class="card">
        <h1 class="page-title">
            <c:out value="${empty errorHeading ? 'Something went wrong' : errorHeading}" />
        </h1>
        <p class="page-subtitle">
            <c:out value="${empty errorMessage
                ? 'Please try again, or contact the front desk if it keeps happening.'
                : errorMessage}" />
        </p>
        <div class="form-actions">
            <a class="btn" href="${ctx}/">Back to the start</a>
            <a class="btn secondary" href="${ctx}/help">Get help</a>
        </div>
    </div>
</div>
</body>
</html>
