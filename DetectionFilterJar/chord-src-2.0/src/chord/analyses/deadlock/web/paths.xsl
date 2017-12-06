<xsl:stylesheet
   version="2.0"
   xmlns="http://www.w3.org/1999/xhtml"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:chord="http://chord.stanford.xsl/">

<xsl:include href="C.xsl"/>
<xsl:include href="I.xsl"/>

<xsl:output omit-xml-declaration="yes"/>

<xsl:function name="chord:printEC">
  <xsl:param name="Melem"/>
  <xsl:param name="Celem"/>
  <tr>
    <td>
      <xsl:variable name="file" select="$Melem/@file"/>
      <xsl:variable name="line" select="$Melem/@line"/>
      <a href="{$file}.html#{$line}"><xsl:value-of select="$file"/>:<xsl:value-of select="$line"/></a> <br/>
      Context: <xsl:apply-templates select="$Celem"/>
    </td>
  </tr>
</xsl:function>

<xsl:template match="/">
  <xsl:for-each select="results/CMCMlist/CMCM">
    <xsl:variable name="C1id" select="@C1id"/>
    <xsl:variable name="M1id" select="@M1id"/>
    <xsl:variable name="C2id" select="@C2id"/>
    <xsl:variable name="M2id" select="@M2id"/>
    <xsl:variable name="C1elem" select="id($C1id)"/>
    <xsl:variable name="M1elem" select="id($M1id)"/>
    <xsl:variable name="C2elem" select="id($C2id)"/>
    <xsl:variable name="M2elem" select="id($M2id)"/>
    <xsl:variable name="filename"
          select="concat('path_', $C1id, '_', $M1id, '_', $C2id, '_', $M2id, '.html')"/>
    <xsl:result-document href="{$filename}">
      <html>
      <head>
        <link rel="stylesheet" href="style.css" type="text/css"/>
      </head>
      <body>
      <xsl:for-each select="path">
        <table class="details">
        <xsl:choose>
          <xsl:when test="truncated">
            <xsl:choose>
              <xsl:when test="truncated/@depth">
                <xsl:variable name="depth" select="truncated/@depth"/>
                <tr><td class="head4">Path (truncated: hit depth limit <xsl:value-of select="$depth"/>)</td></tr>
              </xsl:when>
              <xsl:otherwise>
                <xsl:variable name="width" select="truncated/@width"/>
                <xsl:variable name="limit" select="truncated/@limit"/>
                <tr><td class="head4">Path (truncated: hit width limit <xsl:value-of select="$limit"/>; has width <xsl:value-of select="$width"/>)</td></tr>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <tr><td class="head4">Path (complete)</td></tr>
            <!-- xsl:copy-of select="chord:printEC($M1elem, $C1elem)"/-->
          </xsl:otherwise>
        </xsl:choose>
        <xsl:for-each select="elem">
          <tr>
          <td>
            <xsl:variable name="Ielem" select="id(@Iid)"/>
            <xsl:apply-templates select="$Ielem"/> <br/>
            Context: <xsl:apply-templates select="id(@Cid)"/>
          </td>
          </tr>
        </xsl:for-each>
        <!--xsl:copy-of select="chord:printEC($M2elem, $C2elem)"/-->
        </table>
      </xsl:for-each>
      </body>
      </html>
    </xsl:result-document>
  </xsl:for-each>
</xsl:template>

</xsl:stylesheet>

