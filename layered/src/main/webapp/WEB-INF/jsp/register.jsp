<%-- Patient self-registration. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Create an account — Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${ctx}/css/app.css">
</head>
<body>
<div class="narrow" style="max-width:520px">
    <div class="brand-block">
        <div class="name">Create your account</div>
        <div class="tagline">Book and manage your own appointments</div>
    </div>

    <div class="card">
        <form method="post" action="${ctx}/register">
            <div class="field">
                <label for="name">Full name</label>
                <input type="text" id="name" name="name" required autofocus>
            </div>

            <div class="form-row">
                <div class="field">
                    <label for="email">Email</label>
                    <input type="email" id="email" name="email" required autocomplete="username">
                </div>
                <div class="field">
                    <label for="contactNumber">Contact number</label>
                    <input type="text" id="contactNumber" name="contactNumber" required>
                </div>
            </div>

            <div class="form-row">
                <div class="field">
                    <label for="password">Password</label>
                    <input type="password" id="password" name="password" required
                           minlength="8" autocomplete="new-password">
                    <div class="hint">At least 8 characters.</div>
                </div>
                <div class="field">
                    <label for="confirmPassword">Confirm password</label>
                    <input type="password" id="confirmPassword" name="confirmPassword" required
                           minlength="8" autocomplete="new-password">
                </div>
            </div>

            <div class="field">
                <label for="address">Address <span class="hint">(optional)</span></label>
                <input type="text" id="address" name="address">
            </div>

            <div class="field">
                <label for="dob">Date of birth <span class="hint">(optional)</span></label>
                <input type="date" id="dob" name="dob">
            </div>

            <div class="form-actions">
                <button type="submit" class="btn" style="width:100%">Create account</button>
            </div>
        </form>

        <div class="alt">
            Already registered? <a href="${ctx}/login">Sign in</a>
        </div>
    </div>
</div>
</body>
</html>
