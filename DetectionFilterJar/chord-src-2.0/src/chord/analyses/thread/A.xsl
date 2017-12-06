<xsl:stylesheet
	version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:import href="M.xsl"/>
<xsl:import href="O.xsl"/>
<xsl:import href="C.xsl"/>

<xsl:template match="A">
    <xsl:apply-templates select="id(@Mid)"/> <br/>
    Object:  <xsl:apply-templates select="id(@Oid)"/> <br/>
    Context: <xsl:apply-templates select="id(@Cid)"/>
</xsl:template>

</xsl:stylesheet>

