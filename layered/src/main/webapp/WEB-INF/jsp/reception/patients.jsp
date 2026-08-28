<%-- Patient register: search existing records and add a walk-in. --%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Patients" />
<c:set var="nav" value="patients" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Patient records</h1>
<p class="page-subtitle">Search the register, or add someone who has just walked in.</p>

<c:if test="${not empty param.registered}">
    <div class="notice success">
        <strong><c:out value="${param.registered}" /></strong> has been added to the register.
    </div>
</c:if>

<div class="grid two">
    <div class="card">
        <h2>Search</h2>
        <form method="get" action="${ctx}/reception/patients">
            <div class="field">
                <label for="q">Name, contact number or email</label>
                <input type="search" id="q" name="q" value="<c:out value='${q}' />"
                       placeholder="e.g. Perera or 077…">
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Search</button>
                <c:if test="${not empty q}">
                    <a class="btn secondary" href="${ctx}/reception/patients">Clear</a>
                </c:if>
            </div>
        </form>
    </div>

    <div class="card">
        <h2>Add a patient</h2>
        <form method="post" action="${ctx}/reception/patients">
            <div class="field">
                <label for="name">Full name</label>
                <input type="text" id="name" name="name" required>
            </div>
            <div class="form-row">
                <div class="field">
                    <label for="contactNumber">Contact number</label>
                    <input type="text" id="contactNumber" name="contactNumber" required>
                </div>
                <div class="field">
                    <label for="email">Email <span class="hint">(optional)</span></label>
                    <input type="email" id="email" name="email">
                </div>
            </div>
            <div class="form-row">
                <div class="field">
                    <label for="address">Address <span class="hint">(optional)</span></label>
                    <input type="text" id="address" name="address">
                </div>
                <div class="field">
                    <label for="dob">Date of birth <span class="hint">(optional)</span></label>
                    <input type="date" id="dob" name="dob">
                </div>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn">Add patient</button>
            </div>
        </form>
    </div>
</div>

<div class="card">
    <h2>
        <c:choose>
            <c:when test="${not empty q}">Results for “<c:out value="${q}" />”</c:when>
            <c:otherwise>All patients</c:otherwise>
        </c:choose>
        <span class="count">${patients.size()}</span>
    </h2>

    <c:choose>
        <c:when test="${empty patients}">
            <div class="empty"><p>No patients matched.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Contact</th>
                        <th>Email</th>
                        <th>Date of birth</th>
                        <th>Portal account</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="p" items="${patients}">
                        <tr>
                            <td><strong><c:out value="${p.name}" /></strong></td>
                            <td><c:out value="${p.contactNumber}" /></td>
                            <td><c:out value="${p.email}" /></td>
                            <td>${p.dob}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${not empty p.userUid}">
                                        <span class="pill completed">Yes</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="pill billed">Walk-in</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td><a class="btn secondary small" href="${ctx}/patient/book">Book</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
