<%--
    The administrator's reports.

    Every panel carries a "Supports:" line — FR-ADM-16. A figure with no decision attached
    is trivia, and a report full of trivia is one nobody opens twice. The lines say what
    each number is for.

    The bars are divs sized as a percentage of the period's peak. No chart library, no
    script: the server knows the peak, so the width is arithmetic done in EL.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Reports" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Reports</h1>
<p class="page-subtitle">${report.from()} to ${report.to()}</p>

<div class="card">
    <form method="get" action="${ctx}/admin/reports" class="form-row">
        <div class="field">
            <label for="from">From</label>
            <input type="date" id="from" name="from" value="${from}">
        </div>
        <div class="field">
            <label for="to">To</label>
            <input type="date" id="to" name="to" value="${to}">
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Show</button>
            <a class="btn btn-secondary"
               href="${ctx}/admin/reports.csv?from=${from}&amp;to=${to}">Download CSV</a>
        </div>
    </form>
</div>

<c:choose>
    <%-- FR-ADM-17: say so. An empty chart looks like a broken chart. --%>
    <c:when test="${report.isEmpty()}">
        <div class="card">
            <h2>Nothing happened in this period</h2>
            <p class="page-subtitle">
                No bills were issued and no appointments were attended between
                ${report.from()} and ${report.to()}. Try a wider range.
            </p>
        </div>
    </c:when>
    <c:otherwise>

    <div class="card">
        <h2>Headline</h2>
        <div class="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th class="right">Gross takings</th><th class="right">Bills</th>
                        <th class="right">Patients seen</th><th class="right">On the register</th>
                        <th class="right">No-show rate</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td class="right"><strong>Rs <fmt:formatNumber value="${report.income().gross()}"
                                minFractionDigits="2" maxFractionDigits="2" /></strong></td>
                        <td class="right">${report.income().bills()}</td>
                        <td class="right">${report.patientsSeen()}</td>
                        <td class="right">${report.registeredPatients()}</td>
                        <td class="right">${report.attendance().noShowRate()}%</td>
                    </tr>
                </tbody>
            </table>
        </div>
        <p class="page-subtitle">
            <strong>Supports:</strong> whether the practice is busy enough, and whether the
            register is growing faster than the appointments book.
        </p>
    </div>

    <div class="card">
        <h2>Where the money went</h2>
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr>
                        <th>Dentists</th>
                        <td class="right">Rs <fmt:formatNumber value="${report.income().dentistEarnings()}"
                                minFractionDigits="2" maxFractionDigits="2" /></td>
                    </tr>
                    <tr>
                        <th>The clinic</th>
                        <td class="right"><strong>Rs <fmt:formatNumber value="${report.income().clinicEarnings()}"
                                minFractionDigits="2" maxFractionDigits="2" /></strong></td>
                    </tr>
                    <tr>
                        <th>Reception</th>
                        <td class="right">Rs <fmt:formatNumber value="${report.income().receptionistEarnings()}"
                                minFractionDigits="2" maxFractionDigits="2" /></td>
                    </tr>
                    <tr class="total">
                        <th>Gross takings</th>
                        <td class="right">Rs <fmt:formatNumber value="${report.income().gross()}"
                                minFractionDigits="2" maxFractionDigits="2" /></td>
                    </tr>
                </tbody>
            </table>
        </div>
        <%--
            The attribution must account for every rupee taken. If it ever does not, a bill
            was written under a policy whose shares did not sum, or the query double-counted
            — and either is worth knowing before the figures are used.
        --%>
        <c:if test="${not report.income().isBalanced()}">
            <div class="notice error">
                The attributed shares do not sum to the gross takings. Do not use these
                figures until this is explained.
            </div>
        </c:if>
        <p class="page-subtitle">
            <strong>Supports:</strong> the clinic's own margin, which is what is left after
            the dentists are paid. This is the figure to watch when changing the service
            charge or a dentist's share.
        </p>
    </div>

    <div class="grid two">
        <div class="card">
            <h2>Per dentist</h2>
            <c:choose>
                <c:when test="${empty report.byDentist()}">
                    <p class="page-subtitle">No bills in this period.</p>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>Dentist</th><th class="right">Bills</th>
                                <th class="right">Earned</th></tr></thead>
                            <tbody>
                                <c:forEach var="row" items="${report.byDentist()}">
                                    <tr>
                                        <td><c:out value="${row.name()}" /></td>
                                        <td class="right">${row.count()}</td>
                                        <td class="right">Rs <fmt:formatNumber value="${row.amount()}"
                                                minFractionDigits="2" maxFractionDigits="2" /></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
            <p class="page-subtitle">
                <strong>Supports:</strong> who is carrying the practice, and whether a
                dentist's hours match what they bring in.
            </p>
        </div>

        <div class="card">
            <h2>Per receptionist</h2>
            <c:choose>
                <c:when test="${empty report.byReceptionist()}">
                    <p class="page-subtitle">No bills in this period.</p>
                </c:when>
                <c:otherwise>
                    <div class="table-wrap">
                        <table>
                            <thead><tr><th>Receptionist</th><th class="right">Bills</th>
                                <th class="right">Earned</th></tr></thead>
                            <tbody>
                                <c:forEach var="row" items="${report.byReceptionist()}">
                                    <tr>
                                        <td><c:out value="${row.name()}" /></td>
                                        <td class="right">${row.count()}</td>
                                        <td class="right">Rs <fmt:formatNumber value="${row.amount()}"
                                                minFractionDigits="2" maxFractionDigits="2" /></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:otherwise>
            </c:choose>
            <p class="page-subtitle">
                <strong>Supports:</strong> how the front-desk workload divides. The earned
                column is zero unless the clinic pays handling commission.
            </p>
        </div>
    </div>

    <div class="card">
        <h2>Daily takings</h2>
        <c:set var="peak" value="${report.peakTakings()}" />
        <div class="table-wrap">
            <table>
                <thead><tr><th>Date</th><th class="right">Bills</th>
                    <th class="right">Amount</th><th class="bar-col"></th></tr></thead>
                <tbody>
                    <c:forEach var="point" items="${report.takings()}">
                        <tr>
                            <td>${point.date()}</td>
                            <td class="right">${point.count()}</td>
                            <td class="right">Rs <fmt:formatNumber value="${point.amount()}"
                                    minFractionDigits="2" maxFractionDigits="2" /></td>
                            <td class="bar-col">
                                <div class="bar-track">
                                    <div class="bar-fill"
                                         style="width:${peak gt 0 ? (point.amount() / peak) * 100 : 0}%"></div>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
        <p class="page-subtitle">
            <strong>Supports:</strong> which days are worth staffing fully, and whether a
            quiet stretch is a pattern or a one-off.
        </p>
    </div>

    <div class="card">
        <h2>Footfall and attendance</h2>
        <div class="table-wrap">
            <table>
                <tbody>
                    <tr><th>Attended</th><td class="right">${report.attendance().attended()}</td></tr>
                    <tr><th>Did not attend</th><td class="right">${report.attendance().noShows()}</td></tr>
                    <tr><th>Still upcoming</th><td class="right">${report.attendance().upcoming()}</td></tr>
                    <tr class="total">
                        <th>No-show rate</th>
                        <td class="right"><strong>${report.attendance().noShowRate()}%</strong></td>
                    </tr>
                </tbody>
            </table>
        </div>
        <p class="page-subtitle">
            <strong>Supports:</strong> whether a reminder policy would pay for itself. A
            no-show is an appointment whose date has passed with no treatment recorded and
            no cancellation; upcoming appointments are excluded from the rate.
        </p>
    </div>

    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
