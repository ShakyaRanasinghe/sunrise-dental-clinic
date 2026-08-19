<%--
    Sign-in page.

    Standalone rather than using the shared header, because the navigation would
    be meaningless to someone who is not signed in yet.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in — Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${ctx}/css/app.css">
</head>
<body>
<div class="narrow">
    <div class="brand-block">
        <div class="name">Sunrise Dental Clinic</div>
        <div class="tagline">Appointments &amp; patient records</div>
    </div>

    <div class="card">
        <c:if test="${not empty param.signedOut}">
            <div class="notice success">You have been signed out.</div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="notice error"><c:out value="${error}" /></div>
        </c:if>
        <c:if test="${not empty warning}">
            <div class="notice warning"><c:out value="${warning}" /></div>
        </c:if>

        <form method="post" action="${ctx}/login">
            <c:if test="${not empty next}">
                <input type="hidden" name="next" value="<c:out value='${next}' />">
            </c:if>

            <div class="field">
                <label for="email">Email</label>
                <input type="email" id="email" name="email" required autofocus
                       autocomplete="username" value="<c:out value='${email}' />">
            </div>

            <div class="field">
                <label for="password">Password</label>
                <input type="password" id="password" name="password" required
                       autocomplete="current-password">
            </div>

            <div class="form-actions">
                <button type="submit" class="btn" style="width:100%">Sign in</button>
            </div>
        </form>

        <div class="alt">
            New patient? <a href="${ctx}/register">Create an account</a>
        </div>
    </div>

    <div class="alt"><a href="${ctx}/help">Need help signing in?</a></div>
</div>
</body>
</html>
