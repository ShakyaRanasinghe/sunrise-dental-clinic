<%--
    Administrator reports.

    The bar charts are plain <div> elements sized with an inline width
    percentage — no charting library, and they print and scale correctly.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Reports" />
<c:set var="nav" value="reports" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>

<h1 class="page-title">Clinic reports</h1>
<p class="page-subtitle">Income, revenue split and footfall for the selected period.</p>

<div class="card no-print">
    <form method="get" action="${ctx}/admin/reports">
        <div class="form-row" style="max-width:640px">
            <div class="field" style="margin:0">
                <label for="from">From</label>
                <input type="date" id="from" name="from" value="${from}">
            </div>
            <div class="field" style="margin:0">
                <label for="to">To</label>
                <input type="date" id="to" name="to" value="${to}">
            </div>
            <div class="field" style="margin:0; align-self:end; display:flex; gap:8px">
                <button type="submit" class="btn">Apply</button>
                <a class="btn secondary" href="${ctx}/admin/reports?from=${from}&to=${to}&export=csv">
                    Export CSV
                </a>
            </div>
        </div>
    </form>
</div>

<div class="grid stats">
    <div class="stat">
        <div class="label">Gross takings</div>
        <div class="value money"><fmt:formatNumber value="${income.grossTotal}" pattern="#,##0.00" /></div>
    </div>
    <div class="stat">
        <div class="label">Bills issued</div>
        <div class="value">${income.billCount}</div>
    </div>
    <div class="stat">
        <div class="label">Patients seen</div>
        <div class="value">${footfall.total}</div>
    </div>
    <div class="stat">
        <div class="label">Registered patients</div>
        <div class="value">${patientCount}</div>
    </div>
</div>

<div class="card" style="margin-top:20px">
    <h2>Where the money went</h2>
    <div class="grid stats">
        <div class="stat">
            <div class="label">Dentists</div>
            <div class="value money"><fmt:formatNumber value="${income.dentistEarnings}" pattern="#,##0.00" /></div>
        </div>
        <div class="stat">
            <div class="label">Clinic</div>
            <div class="value money"><fmt:formatNumber value="${income.clinicEarnings}" pattern="#,##0.00" /></div>
        </div>
        <div class="stat">
            <div class="label">Reception</div>
            <div class="value money"><fmt:formatNumber value="${income.receptionistEarnings}" pattern="#,##0.00" /></div>
        </div>
    </div>
    <p class="hint" style="margin-top:14px">
        Dentists receive their consultation fee plus their agreed share of each
        treatment; the clinic retains the remainder; the front desk earns the
        service charge on bills it handles.
    </p>
</div>

<div class="grid two">
    <div class="card">
        <h2>Earnings by dentist</h2>
        <c:choose>
            <c:when test="${empty income.byDentist}">
                <div class="empty"><p>No bills issued in this period.</p></div>
            </c:when>
            <c:otherwise>
                <c:set var="topDentist" value="${income.byDentist[0].amount}" />
                <div class="bars">
                    <c:forEach var="row" items="${income.byDentist}">
                        <div class="row">
                            <div class="name" title="<c:out value='${row.name}' />">
                                <c:out value="${row.name}" />
                            </div>
                            <div class="track">
                                <div class="fill"
                                     style="width:${topDentist > 0 ? (row.amount / topDentist) * 100 : 0}%"></div>
                            </div>
                            <div class="amount">Rs <fmt:formatNumber value="${row.amount}" pattern="#,##0.00" /></div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="card">
        <h2>Earnings by receptionist</h2>
        <c:choose>
            <c:when test="${empty income.byReceptionist}">
                <div class="empty"><p>No bills issued in this period.</p></div>
            </c:when>
            <c:otherwise>
                <c:set var="topStaff" value="${income.byReceptionist[0].amount}" />
                <div class="bars">
                    <c:forEach var="row" items="${income.byReceptionist}">
                        <div class="row">
                            <div class="name" title="<c:out value='${row.name}' />">
                                <c:out value="${row.name}" />
                            </div>
                            <div class="track">
                                <div class="fill"
                                     style="width:${topStaff > 0 ? (row.amount / topStaff) * 100 : 0}%"></div>
                            </div>
                            <div class="amount">Rs <fmt:formatNumber value="${row.amount}" pattern="#,##0.00" /></div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="card">
    <h2>Daily takings</h2>
    <c:choose>
        <c:when test="${empty income.daily}">
            <div class="empty"><p>Nothing billed in this period.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Date</th>
                        <th class="num">Bills</th>
                        <th class="num">Takings</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="point" items="${income.daily}">
                        <tr>
                            <td>${point.date}</td>
                            <td class="num">${point.count}</td>
                            <td class="num">Rs <fmt:formatNumber value="${point.amount}" pattern="#,##0.00" /></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Footfall</h2>
    <c:choose>
        <c:when test="${empty footfall.daily}">
            <div class="empty"><p>No appointments attended in this period.</p></div>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Date</th>
                        <th class="num">Patients seen</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="point" items="${footfall.daily}">
                        <tr>
                            <td>${point.date}</td>
                            <td class="num">${point.count}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
