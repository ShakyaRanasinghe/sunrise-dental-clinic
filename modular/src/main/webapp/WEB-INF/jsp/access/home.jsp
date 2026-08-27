<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Welcome" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<%--
    The public face of the clinic. A first-time visitor lands here knowing nothing:
    this page answers "what is this place, what do they do, who works here, how do
    I reach them" before asking anyone to sign in. The role chooser a returning
    user wants is one click away in the bar above.
--%>

<div class="brand-block" style="text-align:center; margin: 40px 0 28px;">
    <div class="name" style="font-size: 30px; font-weight:700; color: var(--sunrise);">
        <c:out value="${clinicName}" />
    </div>
    <div class="tagline" style="color: var(--ink-faint); margin-top: 6px;">
        Appointments &amp; patient records &mdash; book online, walk in with confidence.
    </div>
    <p style="margin: 18px auto 0;">
        <a class="btn" href="${ctx}/register">Book an appointment</a>
        <a class="btn secondary" href="${ctx}/login/patient">I already have an account</a>
    </p>
</div>

<h2 id="services" class="page-title">Services</h2>
<p class="page-subtitle">What we treat. Sign in to see pricing and book online.</p>

<div class="card">
    <table class="table">
        <thead>
            <tr><th>Treatment</th><th>What it covers</th></tr>
        </thead>
        <tbody>
            <c:forEach var="t" items="${treatments}">
                <tr>
                    <td><c:out value="${t.name()}" /></td>
                    <td><c:out value="${t.description()}" /></td>
                </tr>
            </c:forEach>
        </tbody>
    </table>
</div>

<h2 id="dentists" class="page-title">Our dentists</h2>
<p class="page-subtitle">Every dentist publishes their own availability, so the times you see when booking are real.</p>

<div class="grid two">
    <c:forEach var="d" items="${dentists}">
        <div class="card">
            <h2><c:out value="${d.name()}" /></h2>
            <p><c:out value="${d.specialization()}" /></p>
            <%-- FR-RVW-11: aggregate rating, shown only when 5+ reviews exist (FR-RVW-12) --%>
            <c:set var="rating" value="${ratings[d.id()]}" />
            <c:if test="${not empty rating and rating.isPublishable()}">
                <p style="margin:0;">
                    <span class="stars">
                        <c:forEach begin="1" end="5" var="i">
                            <c:choose>
                                <c:when test="${i <= rating.mean().intValue()}">&#9733;</c:when>
                                <c:otherwise>&#9734;</c:otherwise>
                            </c:choose>
                        </c:forEach>
                    </span>
                    <fmt:formatNumber value="${rating.mean()}" minFractionDigits="1" maxFractionDigits="1" />
                    <span class="page-subtitle">(${rating.reviews()} review${rating.reviews() != 1 ? 's' : ''})</span>
                </p>
            </c:if>
            <c:if test="${not empty rating and not rating.isPublishable() and rating.reviews() > 0}">
                <p style="margin:0;">
                    <span class="page-subtitle">${rating.reviewsUntilPublishable()} more review${rating.reviewsUntilPublishable() != 1 ? 's' : ''} needed to show rating</span>
                </p>
            </c:if>
        </div>
    </c:forEach>
</div>

<h2 id="contact" class="page-title">Visit us</h2>
<div class="card">
    <p><strong><c:out value="${clinicName}" /></strong></p>
    <p>
        <c:out value="${clinicAddress}" /><br>
        Telephone: <c:out value="${clinicPhone}" /><br>
        Email: <c:out value="${clinicEmail}" />
    </p>
    <p class="page-subtitle">
        Prefer to talk first? Call us &mdash; reception can answer questions and book on
        your behalf. Or <a href="${ctx}/register">create an account</a> and choose your own
        time. If you cannot sign in, the <a href="${ctx}/help">help page</a> explains how
        everything works.
    </p>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
