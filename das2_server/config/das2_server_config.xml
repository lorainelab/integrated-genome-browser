
<!-- should really make this an XSLT document and apply transformation to DAS/2 output XML, but for now taking a more primitive route... -->
<TYPE name="cytoBand" />
   <PROP load_hint="Whole Sequence" />
</TYPE>

<TYPE name="refseq" >
   <PROP load_hint="Whole Sequence" />
</TYPE>

<TYPE name="knownGene" />
   <PROP load_hint="Whole Sequence" />
</TYPE>

<TYPE name="mgcGenes" />
   <PROP load_hint="Whole Sequence" />
</TYPE>

<TYPE name="genscan" />
   <PROP load_hint="Whole Sequence" />
</TYPE>


<!-- possible XSLT approach? -->
<!-- identity transform, see http://www.dpawson.co.uk/xsl/sect2/identity.html -->
<!-- <xsl:template match="type[@name='cytoBand' or @name='refseq' or @name='knownGene' or @name='mgcGenes' or @name='genscan']" > -->

<!--  
<xsl:template match="node()|@*">
   <xsl:copy>
   <xsl:apply-templates select="@*"/>
   <xsl:apply-templates/>
   </xsl:copy>
 </xsl:template>

<xsl:template match="/types/type[@name='cytoBand' or @name='refseq' or @name='knownGene' or @name='mgcGenes' or @name='genscan']" >
   <xsl:copy>
       <xsl:apply-templates select="@*|node()"/>
       <PROP key="load_hint" value="Whole Sequence" />
   </xsl:copy>
</xsl:template>

-->