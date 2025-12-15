<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
    xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
    xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ccts="urn:un:unece:uncefact:documentation:2"
    xmlns:qdt="urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2"
    xmlns:udt="urn:oasis:names:specification:ubl:schema:xsd:UnqualifiedDataTypes-2"
    exclude-result-prefixes="xsl xsi ccts qdt udt">

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />

    <!-- =========================
         Helpers: no empty elements
         ========================= -->

    <!-- Emit element with text only if non-empty -->
    <xsl:template name="emit">
        <xsl:param name="qname" />
        <xsl:param name="value" />
        <xsl:if
            test="normalize-space($value) != ''">
            <xsl:element name="{$qname}">
                <xsl:value-of select="$value" />
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- Emit element with text + optional schemeID only if value non-empty -->
    <xsl:template name="emitWithScheme">
        <xsl:param name="qname" />
        <xsl:param name="value" />
        <xsl:param name="schemeID" />
        <xsl:if
            test="normalize-space($value) != ''">
            <xsl:element name="{$qname}">
                <xsl:if test="normalize-space($schemeID) != ''">
                    <xsl:attribute name="schemeID">
                        <xsl:value-of select="$schemeID" />
                    </xsl:attribute>
                </xsl:if>
                <xsl:value-of
                    select="$value" />
            </xsl:element>
        </xsl:if>
    </xsl:template>

    <!-- Utilitaire : normaliser un montant FR vers xs:decimal
         ex: "1 160,55 €" ou "1 160,55 €" -> "1160.55" -->
    <xsl:template name="normalize-amount">
        <xsl:param name="value" />

        <!-- nettoyage de base -->
    <xsl:variable name="trim" select="normalize-space($value)" />

    <xsl:variable
            name="clean"
            select="translate(
                   translate(
                     translate(
                       translate($trim,
                                 concat(' ', '€', '&#160;', '&#x202F;'),
                                 ''
                       ),
                       ',', '.'
                     ),
                     '&#160;', ''
                   ),
                   '&#x202F;', ''
               )" />

    <xsl:choose>

            <!-- valeur vide -->
            <xsl:when test="$clean = ''"> 0.00 </xsl:when>

            <!-- commence par .XXX -->
            <xsl:when test="starts-with($clean, '.')">
                <xsl:value-of select="concat('0', $clean)" />
            </xsl:when>

            <!-- contient un point -->
            <xsl:when test="contains($clean, '.')">
                <xsl:variable name="intPart"
                    select="substring-before($clean, '.')" />
            <xsl:variable name="decPart"
                    select="substring-after($clean, '.')" />

            <xsl:value-of
                    select="concat(
                    $intPart,
                    '.',
                    substring($decPart, 1, 2)
                )" />
            </xsl:when>

            <!-- entier -->
            <xsl:otherwise>
                <xsl:value-of select="$clean" />
            </xsl:otherwise>

        </xsl:choose>
    </xsl:template>

    <!-- Utilitaire : normaliser un taux ex: "20,00%" -> "20.00" -->
    <xsl:template name="normalize-percent">
        <xsl:param name="value" />

        <!-- Nettoyage de base -->
    <xsl:variable name="trim" select="normalize-space($value)" />
    <xsl:variable
            name="noPercent" select="translate($trim, '%', '')" />
    <xsl:variable name="normalized"
            select="translate($noPercent, ',', '.')" />

    <xsl:choose>
            <!-- valeur vide -->
            <xsl:when test="$normalized = ''">
                <xsl:text></xsl:text>
            </xsl:when>

            <!-- contient un point -->
            <xsl:when test="contains($normalized, '.')">
                <xsl:variable name="intPart"
                    select="substring-before($normalized, '.')" />
            <xsl:variable name="decPart"
                    select="substring-after($normalized, '.')" />

                <!-- garder max 2 décimales -->
            <xsl:value-of
                    select="concat(
                    $intPart,
                    '.',
                    substring($decPart, 1, 2)
                )" />
            </xsl:when>

            <!-- entier -->
            <xsl:otherwise>
                <xsl:value-of select="$normalized" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="normalize-quantity">
        <xsl:param name="value" />

        <!-- nettoyage de base -->
    <xsl:variable name="trim" select="normalize-space($value)" />
    <xsl:variable
            name="clean"
            select="translate(
                   translate($trim, ',', '.'),
                   ' ', ''
               )" />

    <xsl:choose>

            <!-- valeur vide -->
            <xsl:when test="$clean = ''"> **INVALID_BT-129_QUANTITY** </xsl:when>

            <!-- commence par -.XXX -->
            <xsl:when test="starts-with($clean, '-.')">
                <xsl:value-of select="concat('-0', substring($clean, 2))" />
            </xsl:when>

            <!-- commence par .XXX -->
            <xsl:when test="starts-with($clean, '.')">
                <xsl:value-of select="concat('0', $clean)" />
            </xsl:when>

            <!-- contient un point -->
            <xsl:when test="contains($clean, '.')">
                <xsl:variable name="intPart"
                    select="substring-before($clean, '.')" />
            <xsl:variable name="decPart"
                    select="substring-after($clean, '.')" />

            <xsl:value-of
                    select="concat(
                    $intPart,
                    '.',
                    substring($decPart, 1, 4)
                )" />
            </xsl:when>

            <!-- entier -->
            <xsl:otherwise>
                <xsl:value-of select="$clean" />
            </xsl:otherwise>

        </xsl:choose>
    </xsl:template>

    <!-- Utilitaire : code pays (FR si FRANCE) -->
    <xsl:template name="country-code">
        <xsl:param name="pays" />
        <xsl:choose>
            <xsl:when
                test="contains(translate(normalize-space($pays), 'france', 'FRANCE'), 'FRANCE')">FR</xsl:when>
            <xsl:when test="normalize-space($pays) != ''">
                <xsl:value-of select="normalize-space($pays)" />
            </xsl:when>
            <xsl:otherwise>FR</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- =========================
         ROOT
         ========================= -->
    <xsl:template match="/">
        <xsl:variable name="facture"
            select="/*/*[local-name()='Numero_de_Facture_LBH_DOC_S6'] | /*[local-name()='Numero_de_Facture_LBH_DOC_S6']" />
        <xsl:variable
            name="enr" select="$facture/*[local-name()='enrichissementUBL']" />
        <xsl:variable
            name="livre" select="$facture/*[local-name()='Client_Livre_LBH_SHAN_S8']" />
        <xsl:variable
            name="sellerName"
            select="normalize-space($facture/*[local-name()='AddrReglementAlphaName'])" />

        <xsl:variable
            name="sellerSiren"
            select="normalize-space($enr/*[local-name()='Vendeur']/*[local-name()='SIREN'])" />

        <xsl:variable
            name="sellerVat"
            select="normalize-space($enr/*[local-name()='Vendeur']/*[local-name()='TVA'])" />

        <Invoice>
            <!-- Header -->
            <cbc:UBLVersionID>2.1</cbc:UBLVersionID>
            <cbc:CustomizationID>urn:cen.eu:en16931:2017</cbc:CustomizationID>
            <cbc:ProfileID>M1</cbc:ProfileID>

            <!-- Identifiants (no empty) -->
            <xsl:call-template name="emit">
                <xsl:with-param name="qname" select="'cbc:ID'" />
                <xsl:with-param name="value" select="$facture/*[local-name()='DocumentNumber']" />
            </xsl:call-template>

            <xsl:call-template name="emit">
                <xsl:with-param name="qname" select="'cbc:IssueDate'" />
                <xsl:with-param name="value" select="$facture/*[local-name()='DateFacture']" />
            </xsl:call-template>

            <xsl:call-template name="emit">
                <xsl:with-param name="qname" select="'cbc:DueDate'" />
                <xsl:with-param name="value" select="$facture/*[local-name()='DateEcheance']" />
            </xsl:call-template>

            <cbc:InvoiceTypeCode>380</cbc:InvoiceTypeCode>

            <!-- Notes fixes (non vides par définition ici) -->
            <cbc:Note>#PMD#Tout retard de paiement engendre une pénalité exigible à compter de la
        date d'échéance, calculée sur la base de trois fois le taux d'intérêt légal.</cbc:Note>
            <cbc:Note>#PMT#Indemnité forfaitaire pour frais de recouvrement en cas de retard de
        paiement : 40 €.</cbc:Note>

            <!-- AAB (no empty) -->
            <xsl:variable name="CGV"
                select="normalize-space($facture/*[local-name()='Message_Condition_générale_de__ID47'])" />
            <xsl:if test="normalize-space($CGV) != ''">
                <cbc:Note>
                    <xsl:value-of select="concat('#AAB#', $CGV)" />
                </cbc:Note>
            </xsl:if>

            <cbc:DocumentCurrencyCode>EUR</cbc:DocumentCurrencyCode>

            <!-- Contract reference (no empty ID) -->
            <xsl:variable name="contractId"
                select="normalize-space($facture/*[local-name()='CodeFactureALKY'])" />
            <xsl:if test="$contractId != ''">
                <cac:ContractDocumentReference>
                    <cbc:ID>
                        <xsl:value-of select="$contractId" />
                    </cbc:ID>
                </cac:ContractDocumentReference>
            </xsl:if>

            <!-- =========================
                 Supplier
                 ========================= -->
            <cac:AccountingSupplierParty>
                <cac:Party>
                    <cbc:EndpointID schemeID="0225">200000008</cbc:EndpointID>

                    <!-- GLN (attention: tu mets schemeID=0209 mais c'est SIRET; je ne change pas ta
                    logique, juste no-empty) -->
                    <xsl:variable name="sellerGln"
                        select="normalize-space($enr/*[local-name()='Vendeur']/*[local-name()='GLN'])" />
                    <xsl:if test="$sellerGln != ''">
                        <cac:PartyIdentification>
                            <cbc:ID schemeID="0209">
                                <xsl:value-of select="$sellerGln" />
                            </cbc:ID>
                        </cac:PartyIdentification>
                    </xsl:if>

                    <!-- SIREN -->
                    <cac:PartyIdentification>
                        <cbc:ID schemeID="0002">
                            <xsl:choose>
                                <xsl:when test="$sellerSiren != ''">
                                    <xsl:value-of select="$sellerSiren" />
                                </xsl:when>
                                <xsl:otherwise>**MISSING_BR-FR-10**</xsl:otherwise>
                            </xsl:choose>
                        </cbc:ID>
                    </cac:PartyIdentification>

                    <!-- PartyName (no empty Name) -->
                    <xsl:if test="$sellerName != ''">
                        <cac:PartyName>
                            <cbc:Name>
                                <xsl:value-of select="$sellerName" />
                            </cbc:Name>
                        </cac:PartyName>
                    </xsl:if>

                    <!-- PostalAddress (only if at least one field exists) -->
                    <xsl:variable name="sellerStreet"
                        select="normalize-space($facture/*[local-name()='AddrReglementLine1'])" />
                    <xsl:variable name="sellerCity"
                        select="normalize-space($facture/*[local-name()='AddrReglementVille'])" />
                    <xsl:variable name="sellerPost"
                        select="normalize-space($facture/*[local-name()='AddrReglementPostalCode'])" />
                    <xsl:variable name="sellerCountry"
                        select="normalize-space($facture/*[local-name()='Papillon_Code_Pays_ID106'])" />

                    <xsl:if
                        test="$sellerStreet != '' or $sellerCity != '' or $sellerPost != '' or $sellerCountry != ''">
                        <cac:PostalAddress>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:StreetName'" />
                                <xsl:with-param name="value" select="$sellerStreet" />
                            </xsl:call-template>

                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:CityName'" />
                                <xsl:with-param name="value" select="$sellerCity" />
                            </xsl:call-template>

                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:PostalZone'" />
                                <xsl:with-param name="value" select="$sellerPost" />
                            </xsl:call-template>

                            <cac:Country>
                                <cbc:IdentificationCode>
                                    <xsl:choose>
                                        <xsl:when test="$sellerCountry != ''"><xsl:value-of
                                                select="$sellerCountry" /></xsl:when>
                                        <xsl:otherwise>FR</xsl:otherwise>
                                    </xsl:choose>
                                </cbc:IdentificationCode>
                            </cac:Country>
                        </cac:PostalAddress>
                    </xsl:if>

                    <!-- VAT (no empty block) -->
                    <xsl:variable name="vatId"
                        select="normalize-space($enr/*[local-name()='Vendeur']/*[local-name()='TVA'])" />

                    <cac:PartyTaxScheme>
                        <cbc:CompanyID>
                            <xsl:choose>
                                <xsl:when test="starts-with($sellerVat, 'FR')">
                                    <xsl:value-of select="$sellerVat" />
                                </xsl:when>
                                <xsl:otherwise>**MISSING_BT-31_VAT**</xsl:otherwise>
                            </xsl:choose>
                        </cbc:CompanyID>
                        <cac:TaxScheme>
                            <cbc:ID>VAT</cbc:ID>
                        </cac:TaxScheme>
                    </cac:PartyTaxScheme>

                    <!-- PartyLegalEntity (no empty children) -->
                    <!-- BT-30 SIREN -->
                    <cac:PartyLegalEntity>
                        <cbc:RegistrationName>
                            <xsl:value-of select="$sellerName" />
                        </cbc:RegistrationName>

                        <!-- BT-30 SIREN -->
                        <cbc:CompanyID schemeID="0002">
                            <xsl:choose>
                                <xsl:when test="string-length($sellerSiren) = 9">
                                    <xsl:value-of select="$sellerSiren" />
                                </xsl:when>
                                <xsl:otherwise>**MISSING_BT-30_SIREN**</xsl:otherwise>
                            </xsl:choose>
                        </cbc:CompanyID>

                        <cbc:CompanyLegalForm>
                            <xsl:value-of select="$facture/*[local-name()='MessageInfoJuridiqueEtAdresseSociete']" />
                        </cbc:CompanyLegalForm>
                    </cac:PartyLegalEntity>

                    <!-- Contact (no empty children) -->
                    <xsl:variable name="contactName"
                        select="normalize-space($facture/*[local-name()='BureauComAddrAlphaName'])" />
                    <xsl:variable name="contactTel"
                        select="normalize-space($facture/*[local-name()='MessageInfoServiceClientTel'])" />
                    <xsl:variable name="contactMail"
                        select="normalize-space($facture/*[local-name()='MessageInfoServiceClientMail'])" />

                    <xsl:if test="$contactName != '' or $contactTel != '' or $contactMail != ''">
                        <cac:Contact>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:Name'" />
                                <xsl:with-param name="value" select="$contactName" />
                            </xsl:call-template>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:Telephone'" />
                                <xsl:with-param name="value" select="$contactTel" />
                            </xsl:call-template>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:ElectronicMail'" />
                                <xsl:with-param name="value" select="$contactMail" />
                            </xsl:call-template>
                        </cac:Contact>
                    </xsl:if>

                </cac:Party>
            </cac:AccountingSupplierParty>

            <!-- =========================
                 Buyer
                 ========================= -->
            <cac:AccountingCustomerParty>
                <cac:Party>
                    <cbc:EndpointID schemeID="0225">100000009_STATUTS</cbc:EndpointID>

                    <xsl:variable name="bStreet"
                        select="normalize-space($facture/*[local-name()='SoldToAddrLine1'])" />
                    <xsl:variable name="bStreet2"
                        select="normalize-space($facture/*[local-name()='SoldToAddrLine2'])" />
                    <xsl:variable name="bStreet3"
                        select="normalize-space($facture/*[local-name()='SoldToAddrLine3'])" />
                    <xsl:variable name="bCity"
                        select="normalize-space($facture/*[local-name()='SoldToAddrVille'])" />
                    <xsl:variable name="bPost"
                        select="normalize-space($facture/*[local-name()='SoldToAddrCodePostal'])" />
                    <xsl:variable name="bCountryRaw"
                        select="normalize-space($facture/*[local-name()='Sold_To_Code_Pays_ID31'])" />

                    <xsl:if
                        test="$bStreet != '' or $bStreet2 != '' or $bStreet3 != '' or $bCity != '' or $bPost != '' or $bCountryRaw != ''">
                        <cac:PostalAddress>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:StreetName'" />
                                <xsl:with-param name="value" select="$bStreet" />
                            </xsl:call-template>

                            <xsl:variable name="bAddStreet">
                                <xsl:choose>
                                    <xsl:when test="$bStreet2 != ''"><xsl:value-of
                                            select="$bStreet2" /></xsl:when>
                                    <xsl:otherwise><xsl:value-of select="$bStreet3" /></xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:AdditionalStreetName'" />
                                <xsl:with-param name="value" select="$bAddStreet" />
                            </xsl:call-template>

                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:CityName'" />
                                <xsl:with-param name="value" select="$bCity" />
                            </xsl:call-template>

                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:PostalZone'" />
                                <xsl:with-param name="value" select="$bPost" />
                            </xsl:call-template>

                            <cac:Country>
                                <cbc:IdentificationCode>
                                    <xsl:call-template name="country-code">
                                        <xsl:with-param name="pays" select="$bCountryRaw" />
                                    </xsl:call-template>
                                </cbc:IdentificationCode>
                            </cac:Country>
                        </cac:PostalAddress>
                    </xsl:if>

                    <xsl:variable name="buyerName"
                        select="normalize-space($facture/*[local-name()='SoldToAddrAlphaName'])" />
                    <xsl:variable name="buyerSiren"
                        select="normalize-space($enr/*[local-name()='Acheteur']/*[local-name()='SIREN'])" />

                    <xsl:if test="$buyerName != '' or $buyerSiren != ''">
                        <cac:PartyLegalEntity>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:RegistrationName'" />
                                <xsl:with-param name="value" select="$buyerName" />
                            </xsl:call-template>

                            <xsl:if test="$buyerSiren != ''">
                                <cbc:CompanyID schemeID="0002">
                                    <xsl:value-of select="$buyerSiren" />
                                </cbc:CompanyID>
                            </xsl:if>
                        </cac:PartyLegalEntity>
                    </xsl:if>

                </cac:Party>
            </cac:AccountingCustomerParty>

            <!-- =========================
                 Delivery
                 ========================= -->
            <xsl:variable name="deliveryId">
                <xsl:choose>
                    <xsl:when
                        test="normalize-space($livre/*[local-name()='ReferenceSurFactureShipTo']) != ''">
                        <xsl:value-of
                            select="normalize-space($livre/*[local-name()='ReferenceSurFactureShipTo'])" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of
                            select="normalize-space($livre/*[local-name()='CodeLivreALKY'])" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:variable name="dStreet"
                select="normalize-space($livre/*[local-name()='ShipToAddrLine1'])" />
            <xsl:variable name="dStreet2"
                select="normalize-space($livre/*[local-name()='ShipToAddrLine2'])" />
            <xsl:variable name="dStreet3"
                select="normalize-space($livre/*[local-name()='ShipToAddrLine3'])" />
            <xsl:variable name="dCity"
                select="normalize-space($livre/*[local-name()='ShipToAddrVille'])" />
            <xsl:variable name="dPost"
                select="normalize-space($livre/*[local-name()='ShipToAddrCodePostal'])" />
            <xsl:variable name="dCountry"
                select="normalize-space($livre/*[local-name()='Ship_To_Code_Pays_ID11'])" />

            <xsl:if
                test="normalize-space($deliveryId) != '' or $dStreet != '' or $dStreet2 != '' or $dStreet3 != '' or $dCity != '' or $dPost != '' or $dCountry != ''">
                <cac:Delivery>
                    <cac:DeliveryLocation>

                        <xsl:if test="normalize-space($deliveryId) != ''">
                            <cbc:ID schemeID="0190">
                                <xsl:value-of select="$deliveryId" />
                            </cbc:ID>
                        </xsl:if>

                        <xsl:if
                            test="$dStreet != '' or $dStreet2 != '' or $dStreet3 != '' or $dCity != '' or $dPost != '' or $dCountry != ''">
                            <cac:Address>
                                <xsl:call-template name="emit">
                                    <xsl:with-param name="qname" select="'cbc:StreetName'" />
                                    <xsl:with-param name="value" select="$dStreet" />
                                </xsl:call-template>

                                <xsl:variable name="dAddStreet">
                                    <xsl:choose>
                                        <xsl:when test="$dStreet2 != ''"><xsl:value-of
                                                select="$dStreet2" /></xsl:when>
                                        <xsl:otherwise><xsl:value-of select="$dStreet3" /></xsl:otherwise>
                                    </xsl:choose>
                                </xsl:variable>
                                <xsl:call-template name="emit">
                                    <xsl:with-param name="qname" select="'cbc:AdditionalStreetName'" />
                                    <xsl:with-param name="value" select="$dAddStreet" />
                                </xsl:call-template>

                                <xsl:call-template name="emit">
                                    <xsl:with-param name="qname" select="'cbc:CityName'" />
                                    <xsl:with-param name="value" select="$dCity" />
                                </xsl:call-template>

                                <xsl:call-template name="emit">
                                    <xsl:with-param name="qname" select="'cbc:PostalZone'" />
                                    <xsl:with-param name="value" select="$dPost" />
                                </xsl:call-template>

                                <cac:Country>
                                    <cbc:IdentificationCode>
                                        <xsl:choose>
                                            <xsl:when test="$dCountry != ''"><xsl:value-of
                                                    select="$dCountry" /></xsl:when>
                                            <xsl:otherwise>FR</xsl:otherwise>
                                        </xsl:choose>
                                    </cbc:IdentificationCode>
                                </cac:Country>
                            </cac:Address>
                        </xsl:if>

                    </cac:DeliveryLocation>
                </cac:Delivery>
            </xsl:if>

            <!-- =========================
                 PaymentMeans / Terms
                 ========================= -->
            <xsl:variable name="pmtCode">
                <xsl:choose>
                    <xsl:when
                        test="normalize-space($facture/*[local-name()='CodeModePaiement_ID133']) = 'S'">30 </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of
                            select="normalize-space($facture/*[local-name()='CodeModePaiement_ID133'])" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="iban"
                select="normalize-space($facture/*[local-name()='CodeIBANBurCom'])" />
            <xsl:variable name="bic"
                select="normalize-space($facture/*[local-name()='CodeBICBurCom'])" />

            <xsl:if test="$pmtCode != '' or $iban != '' or $bic != '' or $sellerName != ''">
                <cac:PaymentMeans>
                    <xsl:if test="$pmtCode != ''">
                        <cbc:PaymentMeansCode>
                            <xsl:value-of select="$pmtCode" />
                        </cbc:PaymentMeansCode>
                    </xsl:if>

                    <xsl:if test="$iban != '' or $bic != '' or $sellerName != ''">
                        <cac:PayeeFinancialAccount>
                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:ID'" />
                                <xsl:with-param name="value" select="$iban" />
                            </xsl:call-template>

                            <xsl:call-template name="emit">
                                <xsl:with-param name="qname" select="'cbc:Name'" />
                                <xsl:with-param name="value" select="$sellerName" />
                            </xsl:call-template>

                            <xsl:if test="$bic != ''">
                                <cac:FinancialInstitutionBranch>
                                    <cbc:ID>
                                        <xsl:value-of select="$bic" />
                                    </cbc:ID>
                                </cac:FinancialInstitutionBranch>
                            </xsl:if>

                        </cac:PayeeFinancialAccount>
                    </xsl:if>
                </cac:PaymentMeans>
            </xsl:if>

            <xsl:variable name="pmtDesc"
                select="normalize-space($facture/*[local-name()='DescriptionModePaiement'])" />
            <xsl:if test="$pmtDesc != ''">
                <cac:PaymentTerms>
                    <cbc:Note>
                        <xsl:value-of select="$pmtDesc" />
                    </cbc:Note>
                </cac:PaymentTerms>
            </xsl:if>

            <!-- =========================
                 TaxTotal (kept as you had; Percent normalized & no empties)
                 ========================= -->
            <cac:TaxTotal>
                <cbc:TaxAmount currencyID="EUR">
                    <xsl:call-template name="normalize-amount">
                        <xsl:with-param name="value"
                            select="$facture/*[local-name()='MontantTotalTVA']" />
                    </xsl:call-template>
                </cbc:TaxAmount>

                <xsl:for-each select="$facture/*[local-name()='LigneRecapTVA']">
                    <xsl:variable name="tauxRaw" select="normalize-space(*[local-name()='TauxTVA'])" />

                    <xsl:variable
                        name="tauxNorm">
                        <xsl:choose>

                            <!-- vide -->
                            <xsl:when test="normalize-space($tauxRaw) = ''">
        **INVALID_BT-152_TVA_RATE** </xsl:when>

                            <!-- pas un nombre -->
                            <xsl:when test="not(number($tauxRaw) = number($tauxRaw))">
        **INVALID_BT-152_TVA_RATE** </xsl:when>

                            <!-- OK -->
                            <xsl:otherwise>
                                <xsl:value-of select="$tauxRaw" />
                            </xsl:otherwise>

                        </xsl:choose>
                    </xsl:variable>

                    <xsl:variable
                        name="taxableAmount" select="*[local-name()='MontantHTAvecTVA']" />
                    <xsl:variable
                        name="montantTva" select="*[local-name()='MontantLigneTVA']" />

                    <cac:TaxSubtotal>
                        <cbc:TaxableAmount currencyID="EUR">
                            <xsl:call-template name="normalize-amount">
                                <xsl:with-param name="value" select="$taxableAmount" />
                            </xsl:call-template>
                        </cbc:TaxableAmount>
                        <cbc:TaxAmount currencyID="EUR">
                            <xsl:call-template name="normalize-amount">
                                <xsl:with-param name="value" select="$montantTva" />
                            </xsl:call-template>
                        </cbc:TaxAmount>
                        <cac:TaxCategory>
                            <cbc:ID>S</cbc:ID>
                            <xsl:if test="normalize-space($tauxNorm) != ''">
                                <cbc:Percent>
                                    <xsl:value-of select="normalize-space($tauxNorm)" />
                                </cbc:Percent>
                            </xsl:if>
                            <cac:TaxScheme>
                                <cbc:ID>VAT</cbc:ID>
                            </cac:TaxScheme>
                        </cac:TaxCategory>
                    </cac:TaxSubtotal>
                </xsl:for-each>
            </cac:TaxTotal>

            <!-- =========================
                 Monetary totals (you kept 0.00 default in normalize-amount)
                 ========================= -->
            <cac:LegalMonetaryTotal>
                <cbc:LineExtensionAmount currencyID="EUR">
                    <xsl:call-template name="normalize-amount">
                        <xsl:with-param name="value"
                            select="$facture/*[local-name()='TotalMontantHT']" />
                    </xsl:call-template>
                </cbc:LineExtensionAmount>
                <cbc:TaxExclusiveAmount currencyID="EUR">
                    <xsl:call-template name="normalize-amount">
                        <xsl:with-param name="value"
                            select="$facture/*[local-name()='TotalMontantHT']" />
                    </xsl:call-template>
                </cbc:TaxExclusiveAmount>
                <cbc:TaxInclusiveAmount currencyID="EUR">
                    <xsl:call-template name="normalize-amount">
                        <xsl:with-param name="value" select="$facture/*[local-name()='MontantTTC']" />
                    </xsl:call-template>
                </cbc:TaxInclusiveAmount>
                <cbc:PayableAmount currencyID="EUR">
                    <xsl:call-template name="normalize-amount">
                        <xsl:with-param name="value" select="$facture/*[local-name()='MontantTTC']" />
                    </xsl:call-template>
                </cbc:PayableAmount>
            </cac:LegalMonetaryTotal>

            <!-- =========================
                 Lines (no empty qty/percent/price)
                 ========================= -->
            <xsl:for-each
                select="$facture/*[local-name()='Client_Livre_LBH_SHAN_S8']/*[local-name()='Detail_ligne_article_S2']">

                <xsl:variable name="libelle"
                    select="normalize-space(*[local-name()='DescriptionLine1_ID278'])" />
                <xsl:variable
                    name="article"
                    select="normalize-space(*[local-name()='ItemNoUnknownFormat_ID282'])" />
                <xsl:variable
                    name="dateLivraison"
                    select="normalize-space(*[local-name()='Date_Livraison_ID340'])" />
                <xsl:variable
                    name="bareme"
                    select="normalize-space(*[local-name()='szIntituleBareme_RMK3_ID324'])" />
                <xsl:variable
                    name="nbLitres"
                    select="normalize-space(*[local-name()='Nb_Litres_Livres_ID344'])" />
                <xsl:variable
                    name="temperature"
                    select="normalize-space(*[local-name()='Temperature_Livr_IR04_ID346'])" />
                <xsl:variable
                    name="masseVolumique"
                    select="normalize-space(*[local-name()='Masse_Vol_IR05_ID348'])" />
                <xsl:variable
                    name="quantite"
                    select="normalize-space(*[local-name()='Poids_Quantité_Livrée_UORG_ID354'])" />
                <xsl:variable
                    name="UM" select="normalize-space(*[local-name()='szDescUMShort_AA03_ID380'])" />

                <xsl:variable
                    name="montantHT" select="*[local-name()='MntHT_ID356']" />
                <xsl:variable
                    name="prixUnitaireRaw"
                    select="normalize-space(*[local-name()='Prix_PrixNet_UPRC_ID352'])" />

                <xsl:variable
                    name="montantRemiseRaw"
                    select="normalize-space(*[local-name()='Remise_exceptionnelle_S14']/*[local-name()='mnMontantUnitaireRemiseExcep_F_ID8'])" />

                <xsl:variable
                    name="tauxTaxe">
                    <xsl:call-template name="normalize-percent">
                        <xsl:with-param name="value"
                            select="normalize-space(*[local-name()='AZ_Taux_de_TVA_ID250'])" />
                    </xsl:call-template>
                </xsl:variable>

                <cac:InvoiceLine>
                    <cbc:ID>
                        <xsl:number level="any" count="*[local-name()='Detail_ligne_article_S2']"
                            format="1" />
                    </cbc:ID>

                    <!-- InvoicedQuantity: only if quantity exists; omit unitCode if empty -->
                    <xsl:variable name="unitCode">
                        <xsl:choose>
                            <xsl:when test="$UM = 'T' or $UM = 'TONNE'">TNE</xsl:when>
                            <xsl:when test="$UM = 'L'">LTR</xsl:when>
                            <xsl:when test="$UM = 'KG'">KGM</xsl:when>
                            <xsl:when test="$UM = 'UN'">C62</xsl:when>
                            <xsl:otherwise><xsl:value-of select="$UM" /></xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>

                    <xsl:if test="$quantite != ''">
                        <cbc:InvoicedQuantity>
                            <xsl:if test="$UM != ''">
                                <xsl:attribute name="unitCode"><xsl:value-of select="$unitCode" /></xsl:attribute>
                            </xsl:if>
                            <xsl:call-template name="normalize-amount">
                                <xsl:with-param name="value" select="$quantite" />
                            </xsl:call-template>
                        </cbc:InvoicedQuantity>
                    </xsl:if>

                    <cbc:LineExtensionAmount currencyID="EUR">
                        <xsl:call-template name="normalize-amount">
                            <xsl:with-param name="value" select="$montantHT" />
                        </xsl:call-template>
                    </cbc:LineExtensionAmount>

                    <cac:Item>
                        <xsl:if test="$libelle != ''">
                            <cbc:Name>
                                <xsl:value-of select="$libelle" />
                            </cbc:Name>
                        </xsl:if>

                        <cac:ClassifiedTaxCategory>
                            <cbc:ID>S</cbc:ID>
                            <xsl:if test="normalize-space($tauxTaxe) != ''">
                                <cbc:Percent>
                                    <xsl:value-of select="normalize-space($tauxTaxe)" />
                                </cbc:Percent>
                            </xsl:if>
                            <cac:TaxScheme>
                                <cbc:ID>VAT</cbc:ID>
                            </cac:TaxScheme>
                        </cac:ClassifiedTaxCategory>

                        <xsl:if test="$article = '3'">
                            <xsl:if test="$dateLivraison != ''">
                                <cac:AdditionalItemProperty>
                                    <cbc:Name>Livraison du</cbc:Name>
                                    <cbc:Value>
                                        <xsl:value-of select="$dateLivraison" />
                                    </cbc:Value>
                                </cac:AdditionalItemProperty>
                            </xsl:if>

                            <xsl:if
                                test="$bareme != ''">
                                <cac:AdditionalItemProperty>
                                    <cbc:Name>Barème</cbc:Name>
                                    <cbc:Value>
                                        <xsl:value-of select="$bareme" />
                                    </cbc:Value>
                                </cac:AdditionalItemProperty>
                            </xsl:if>

                            <xsl:if
                                test="$nbLitres != ''">
                                <cac:AdditionalItemProperty>
                                    <cbc:Name>Quantité</cbc:Name>
                                    <cbc:Value>
                                        <xsl:value-of
                                            select="concat($nbLitres, ' litres livrés à ', $temperature, '°C', ' (masse volumique = ', $masseVolumique, ')')" />
                                    </cbc:Value>
                                </cac:AdditionalItemProperty>
                            </xsl:if>
                        </xsl:if>

                    </cac:Item>

                    <cac:Price>
                        <!-- PriceAmount: only if non-empty -->
                        <xsl:if test="$prixUnitaireRaw != ''">
                            <cbc:PriceAmount currencyID="EUR">
                                <xsl:call-template name="normalize-amount">
                                    <xsl:with-param name="value" select="$prixUnitaireRaw" />
                                </xsl:call-template>
                            </cbc:PriceAmount>
                        </xsl:if>

                        <cbc:BaseQuantity>
                            <xsl:attribute name="unitCode">
                                <xsl:value-of select="$unitCode" />
                            </xsl:attribute>
        1.0000 </cbc:BaseQuantity>

                        <!-- AllowanceCharge only if remise > 0 -->
                        <xsl:if test="number($montantRemiseRaw) &gt; 0">
                            <cac:AllowanceCharge>
                                <cbc:ChargeIndicator>false</cbc:ChargeIndicator>

                                <cbc:Amount currencyID="EUR">
                                    <xsl:call-template name="normalize-amount">
                                        <xsl:with-param name="value" select="$montantRemiseRaw" />
                                    </xsl:call-template>
                                </cbc:Amount>

                                <cbc:BaseAmount currencyID="EUR">
                                    <xsl:call-template name="normalize-amount">
                                        <xsl:with-param name="value"
                                            select="number($prixUnitaireRaw) + number($montantRemiseRaw)" />
                                    </xsl:call-template>
                                </cbc:BaseAmount>
                            </cac:AllowanceCharge>
                        </xsl:if>
                    </cac:Price>
                </cac:InvoiceLine>
            </xsl:for-each>

        </Invoice>
    </xsl:template>

</xsl:stylesheet>