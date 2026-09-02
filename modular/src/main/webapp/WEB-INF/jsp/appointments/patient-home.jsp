<%--
    The patient's own dashboard.

    No patient id anywhere on this page. Their appointments are resolved from the
    signed-in account, so there is no parameter to change to see someone else's.
--%>
<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="My appointments" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<h1 class="page-title">My appointments</h1>
<p class="page-subtitle">Book a visit, or cancel one you can no longer make.</p>

<c:if test="${not empty booked}">
    <div class="notice">
        Booked. Your appointment number is <strong><c:out value="${booked}" /></strong> &mdash;
        quote it if you telephone the clinic.
    </div>
</c:if>
<c:if test="${not empty cancelled}">
    <div class="notice">Cancelled <strong><c:out value="${cancelled}" /></strong>.</div>
</c:if>
<c:if test="${not empty rated}">
    <div class="notice">Thank you. Your rating has been recorded.</div>
</c:if>

<div class="card">
    <h2>Upcoming and past <span class="count">${fn:length(appointments)}</span></h2>

    <c:choose>
        <c:when test="${empty appointments}">
            <p class="page-subtitle">You have no appointments yet.</p>
            <p class="form-actions"><a class="btn" href="${ctx}/patient/book">Book an appointment</a></p>
        </c:when>
        <c:otherwise>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Number</th><th>Date</th><th>Time</th>
                            <th>Dentist</th><th>Treatment</th><th>Status</th>
                            <th>Dentist's comment</th><th></th><th></th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="a" items="${appointments}">
                            <tr>
                                <td><code>${a.appointmentNo()}</code></td>
                                <td>${a.date()}</td>
                                <td>${a.time()}</td>
                                <td><c:out value="${a.dentistName()}" /></td>
                                <td><c:out value="${a.treatmentName()}" /></td>
                                <td>
                                    <span class="pill <c:choose>
                                        <c:when test="${a.status() eq 'CONFIRMED'}">open</c:when>
                                        <c:when test="${a.status() eq 'CANCELLED'}">error</c:when>
                                    </c:choose>">${a.status()}</span>
                                </td>
                                <td>
                                    <%-- GAP-DEN-09: the treating dentist's comment, visible
                                         only to the patient (ClinicAccess gates the diagnosis
                                         field on AppointmentDetailResponse). Shown once
                                         recorded; left blank while the visit is still ahead. --%>
                                    <c:if test="${not empty a.diagnosis()}">
                                        <span class="page-subtitle"><c:out value="${a.diagnosis()}" /></span>
                                    </c:if>
                                </td>
                                <td>
                                    <%-- Shown only when the status machine permits it, so the
                                         page never offers a move the service would refuse.
                                         GAP-PAT-21: cancelling must not commit on one click.
                                         The <details>/<summary> pair (no script, per the stack
                                         rule) makes the desk-style "Cancel" open a second,
                                         explicit "Yes, cancel" submit — a stray click can no
                                         longer drop a confirmed slot. --%>
                                    <c:if test="${a.isCancellable()}">
                                        <details class="confirm-cancel">
                                            <summary class="btn small secondary">Cancel</summary>
                                            <div class="confirm-cancel__panel">
                                                <p class="page-subtitle">Cancel appointment ${a.appointmentNo()}? This releases the slot.</p>
                                                <form method="post" action="${ctx}/patient/home">
                                                    <input type="hidden" name="appointmentNo"
                                                           value="${a.appointmentNo()}">
                                                    <button type="submit" class="btn small">Yes, cancel it</button>
                                                </form>
                                            </div>
                                        </details>
                                    </c:if>
                                </td>
                                <%-- FR-PAT-70: star rating for COMPLETED/BILLED appointments --%>
                                <td>
                                    <c:if test="${a.status() eq 'COMPLETED' or a.status() eq 'BILLED'}">
                                        <c:set var="existingReview" value="${existingReviews[a.appointmentNo()]}" />
                                        <c:choose>
                                            <c:when test="${not empty existingReview}">
                                                <span class="stars" title="${existingReview.rating()} of 5 stars">
                                                    <c:forEach begin="1" end="5" var="i">
                                                        <c:choose>
                                                            <c:when test="${i <= existingReview.rating()}">&#9733;</c:when>
                                                            <c:otherwise>&#9734;</c:otherwise>
                                                        </c:choose>
                                                    </c:forEach>
                                                </span>
                                                <c:if test="${existingReview.editable()}">
                                                    <form method="post" action="${ctx}/patient/home"
                                                          style="display:inline">
                                                        <input type="hidden" name="action" value="rate">
                                                        <input type="hidden" name="appointmentNo"
                                                               value="${a.appointmentNo()}">
                                                        <select name="rating" style="font-size:0.85em;">
                                                            <c:forEach begin="1" end="5" var="i">
                                                                <option value="${i}" <c:if test="${i == existingReview.rating()}">selected</c:if>>${i} star${i > 1 ? 's' : ''}</option>
                                                            </c:forEach>
                                                        </select>
                                                        <input type="text" name="comment" placeholder="Comment (optional)"
                                                               value="<c:out value='${existingReview.comment()}' />"
                                                               style="font-size:0.85em; width:120px;">
                                                        <button type="submit" class="btn small">Update</button>
                                                    </form>
                                                </c:if>
                                            </c:when>
                                            <c:otherwise>
                                                <form method="post" action="${ctx}/patient/home"
                                                      style="display:inline">
                                                    <input type="hidden" name="action" value="rate">
                                                    <input type="hidden" name="appointmentNo"
                                                           value="${a.appointmentNo()}">
                                                    <select name="rating" required style="font-size:0.85em;">
                                                        <option value="">Rate&hellip;</option>
                                                        <option value="1">&#9733;</option>
                                                        <option value="2">&#9733;&#9733;</option>
                                                        <option value="3">&#9733;&#9733;&#9733;</option>
                                                        <option value="4">&#9733;&#9733;&#9733;&#9733;</option>
                                                        <option value="5">&#9733;&#9733;&#9733;&#9733;&#9733;</option>
                                                    </select>
                                                    <input type="text" name="comment" placeholder="Comment (optional)"
                                                           style="font-size:0.85em; width:120px;">
                                                    <button type="submit" class="btn small">Rate</button>
                                                </form>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:if>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <p class="form-actions"><a class="btn" href="${ctx}/patient/book">Book another</a></p>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
