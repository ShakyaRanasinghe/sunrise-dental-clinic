<%--
    Publish availability.

    The panel below the form shows every slot for the dentist and date being looked
    at, booked ones included. That is deliberate: the service refuses a window that
    overlaps one already published - because a deterministic slot id turns an overlap
    into a silent overwrite, and a slot already booked would be reset to open - so the
    screen has to show what is there before the refusal happens.

    Response records are read by calling their accessors as methods: Jakarta EL 5.0
    resolves getName(), not a record's name().
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Publish availability" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">Publish availability</h1>
<p class="page-subtitle">Set the hours a dentist works; the bookable times are generated for you.</p>

<%-- GAP-FTB-14: publishing confirms in a sub-window. Warnings and errors stay
     inline notices — they need the form context beneath them. --%>
<c:if test="${not empty confirmation}">
    <div class="sub-window open" role="dialog" aria-modal="true" aria-labelledby="published-title">
        <div class="dialog">
            <span class="success-tick" aria-hidden="true">&#10003;</span>
            <h3 id="published-title">Successfully published</h3>
            <p><c:out value="${confirmation}" /></p>
            <div class="form-actions">
                <a class="btn" href="${ctx}/reception/availability">Close</a>
            </div>
        </div>
    </div>
</c:if>
<c:if test="${not empty warning}">
    <div class="notice error"><c:out value="${warning}" /></div>
</c:if>
<c:if test="${not empty error}">
    <div class="notice error">
        <strong>Could not publish.</strong> <c:out value="${error}" />
        <c:if test="${not empty published}">
            The existing sessions for this dentist and date are shown below.
        </c:if>
    </div>
</c:if>

<%-- ── Upcoming sessions overview ──────────────────────────────────── --%>
<div class="card">
    <h2>Already published <span class="count">${fn:length(upcomingSessions)}</span></h2>
    <c:choose>
        <c:when test="${empty upcomingSessions}">
            <p class="page-subtitle">Nothing published from today onwards yet.</p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Dentist</th>
                            <th>Date</th>
                            <th>Window</th>
                            <th>Slot length</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${upcomingSessions}">
                            <tr>
                                <td><c:out value="${row.dentistName()}" /></td>
                                <td>${row.session().getDate()}</td>
                                <td>${row.session().getStartTime()} &ndash; ${row.session().getEndTime()}</td>
                                <td>${row.session().getSlotDurationMinutes()} min</td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>New session</h2>
    <form method="post" action="${ctx}/reception/availability">
        <div class="form-row">
            <div class="field">
                <label for="dentistId">Dentist</label>
                <select id="dentistId" name="dentistId" required>
                    <option value="">Choose&hellip;</option>
                    <c:forEach var="d" items="${dentists}">
                        <option value="<c:out value='${d.id()}' />"
                            <c:if test="${d.id() eq dentistId}">selected</c:if>>
                            <c:out value="${d.name()}" />
                            <c:if test="${not empty d.specialization()}">
                                &mdash; <c:out value="${d.specialization()}" />
                            </c:if>
                        </option>
                    </c:forEach>
                </select>
            </div>
            <div class="field">
                <label for="date">Date</label>
                <input type="date" id="date" name="date" value="${date}" required>
            </div>
        </div>
        <div class="form-row">
            <div class="field">
                <label for="startTime">From</label>
                <input type="time" id="startTime" name="startTime" value="09:00" required>
            </div>
            <div class="field">
                <label for="endTime">To</label>
                <input type="time" id="endTime" name="endTime" value="12:00" required>
            </div>
            <div class="field">
                <label for="slotMinutes">Each appointment</label>
                <select id="slotMinutes" name="slotMinutes">
                    <option value="30" selected>30 minutes</option>
                    <option value="20">20 minutes</option>
                    <option value="45">45 minutes</option>
                    <option value="60">60 minutes</option>
                </select>
            </div>
        </div>
        <div class="form-actions">
            <button type="submit" class="btn">Publish</button>
        </div>
    </form>
</div>

<div class="card">
    <h2>
        Published
        <c:if test="${not empty dentistName}">
            <span class="count"><c:out value="${dentistName}" />, ${date}</span>
        </c:if>
    </h2>

    <c:choose>
        <c:when test="${empty dentistId}">
            <p class="page-subtitle">Choose a dentist and a date to see what is already published.</p>
        </c:when>
        <c:when test="${empty published}">
            <p class="page-subtitle">
                Nothing published for <c:out value="${dentistName}" /> on ${date} yet.
            </p>
            <%-- Look at another date without publishing anything. --%>
            <form method="get" action="${ctx}/reception/availability" class="form-row">
                <input type="hidden" name="dentistId" value="<c:out value='${dentistId}' />">
                <div class="field">
                    <label for="lookDate">Look at another date</label>
                    <input type="date" id="lookDate" name="date" value="${date}">
                </div>
                <div class="form-actions"><button type="submit" class="btn btn-secondary">Show</button></div>
            </form>
        </c:when>
        <c:otherwise>
            <div class="slots">
                <c:forEach var="slot" items="${published}">
                    <div class="slot">
                        <input type="checkbox" id="s-${slot.id()}" disabled
                               <c:if test="${slot.status() eq 'BOOKED'}">checked</c:if>>
                        <label for="s-${slot.id()}">${slot.startTime()}</label>
                    </div>
                </c:forEach>
            </div>
            <p class="page-subtitle">
                Ticked times are already booked. ${fn:length(published)} times in total.
            </p>
            <form method="get" action="${ctx}/reception/availability" class="form-row">
                <input type="hidden" name="dentistId" value="<c:out value='${dentistId}' />">
                <div class="field">
                    <label for="lookDate2">Look at another date</label>
                    <input type="date" id="lookDate2" name="date" value="${date}">
                </div>
                <div class="form-actions"><button type="submit" class="btn btn-secondary">Show</button></div>
            </form>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
