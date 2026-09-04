<%@ include file="/WEB-INF/jsp/shared/taglibs.jspf" %>
<c:set var="pageTitle" value="Welcome" />
<%@ include file="/WEB-INF/jsp/shared/header.jspf" %>

<%--
    The public face of the clinic. A first-time visitor lands here knowing nothing:
    this page answers "what is this place, what do they do, who works here, how do
    I reach them" before asking anyone to sign in. The role chooser a returning
    user wants is one click away in the bar above.
--%>

<div class="hero" style="background-image: url('${ctx}/images/hero-banner.jpg');">
    <a class="btn" href="${ctx}/register">Book an appointment</a>
    <a class="btn secondary" href="${ctx}/login/patient">I already have an account</a>
</div>

<h2 id="services" class="page-title">Services</h2>
<p class="page-subtitle">What we treat. Open a service to see what it covers. Sign in to see pricing and book online.</p>

<div class="card services-list">
    <c:forEach var="t" items="${treatments}" varStatus="i">
        <details class="service-item"<c:if test="${i.first}"> open</c:if>>
            <summary>
                <span class="service-item__no">0${i.index + 1}</span>
                <span class="service-item__name"><c:out value="${t.name()}" /></span>
            </summary>
            <div class="service-item__body">
                <p class="service-item__desc"><c:out value="${t.description()}" /></p>
            </div>
        </details>
    </c:forEach>
</div>

<h2 id="dentists" class="page-title">Our dentists</h2>
<p class="page-subtitle">Every dentist publishes their own availability, so the times you see when booking are real.</p>

<div class="grid two">
    <c:forEach var="d" items="${dentists}" varStatus="i">
        <div class="dentist-card">
            <c:set var="nameWords" value="${fn:split(d.name(), ' ')}" />
            <c:set var="wordCount" value="${fn:length(nameWords)}" />
            <div class="dentist-card__summary">
                <span class="dentist-card__avatar"
                      aria-hidden="true">${wordCount > 1 ? fn:substring(nameWords[1], 0, 1) : fn:substring(nameWords[0], 0, 1)}${wordCount > 1 ? fn:substring(nameWords[wordCount - 1], 0, 1) : ''}</span>
                <span class="dentist-card__who">
                    <span class="dentist-card__name"><c:out value="${d.name()}" /></span>
                    <span class="dentist-card__spec"><c:out value="${d.specialization()}" /></span>
                </span>
            </div>
            <div class="dentist-card__body">
                <c:set var="rating" value="${ratings[d.id()]}" />
                <%-- FR-RVW-11: aggregate rating, shown only when 5+ reviews exist (FR-RVW-12) --%>
                <c:if test="${not empty rating and rating.isPublishable()}">
                    <p class="dentist-card__rating">
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
                <%-- Below the publishable threshold (FR-RVW-12) no rating metric
                     shows at all — neither stars nor a "more reviews needed" hint. --%>
                <c:if test="${not empty d.phone()}">
                    <p class="dentist-card__phone">
                        <c:out value="${d.phone()}" />
                    </p>
                </c:if>
                <p class="dentist-card__fee">
                    Consultation
                    <strong>Rs <fmt:formatNumber value="${d.consultationFee()}" minFractionDigits="2" maxFractionDigits="2" /></strong>
                </p>
                <a class="btn" href="${ctx}/register">Register &amp; book online</a>
            </div>
        </div>
    </c:forEach>
</div>

<%@ include file="/WEB-INF/jsp/shared/footer.jspf" %>
