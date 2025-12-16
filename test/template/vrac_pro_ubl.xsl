<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
    xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
    xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ccts="urn:un:unece:uncefact:documentation:2"
    xmlns:qdt="urn:oasis:names:specification:ubl:schema:xsd:QualifiedDatatypes-2"
    xmlns:udt="urn:oasis:names:specification:ubl:schema:xsd:UnqualifiedDataTypes-2"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:ubl="urn:nomana:ubl:common"
    exclude-result-prefixes="xsl xsi ccts qdt udt xs ubl">

    <xsl:import href="ubl-common.xsl" />
    <xsl:import href="ubl-defaults.xsl" />

    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />

    <!-- ROOT -->
    <xsl:template match="/">
        <!-- DEFINE TOP-LEVEL VARIABLES-->
        <xsl:variable name="defaultCurrency" select="'EUR'" />

        <xsl:variable
            name="invoiceGroup"
            select="/*/*[local-name()='Numero_de_Facture_LBH_DOC_S6'] | /*[local-name()='Numero_de_Facture_LBH_DOC_S6']" />
        <xsl:variable
            name="shipToGroup"
            select="$invoiceGroup/*[local-name()='Client_Livre_LBH_SHAN_S8']" />
        <xsl:variable
            name="documentNumber"
            select="$invoiceGroup/*[local-name()='DocumentNumber']" />   
        <xsl:variable
            name="issueDate"
            select="$invoiceGroup/*[local-name()='DateFacture']" />   
        <xsl:variable
            name="dueDate"
            select="$invoiceGroup/*[local-name()='DateEcheance']" />
        <!-- TO BE DEFINED -->
        <xsl:variable
            name="invoiceTypeCode"
            select="''" />   
        <xsl:variable
            name="CGV"
            select="$invoiceGroup/*[local-name()='Message_Condition_générale_de__ID47']" />
        <!-- TO BE DEFINED -->
        <xsl:variable
            name="currencyCode"
            select="''" />   
        <xsl:variable
            name="contractID"
            select="$invoiceGroup/*[local-name()='CodeFactureALKY']" />
        <!-- TO BE DEFINED -->
        <xsl:variable
            name="endPointID"
            select="''" />

        <!-- END OF TOP-LEVEL VARIABLES -->

        <Invoice>
            <!-- Header -->
            <xsl:call-template name="ubl:invoice-header" />

            <!-- Identifiants -->
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:ID'" />
                <xsl:with-param name="value" select="$documentNumber" />
            </xsl:call-template>

            <!-- Issue Date -->
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:IssueDate'" />
                <xsl:with-param name="value" select="$issueDate" />
            </xsl:call-template>

            <!-- Due Date -->
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:DueDate'" />
                <xsl:with-param name="value" select="$dueDate" />
            </xsl:call-template>

            <!-- Invoice Type Code -->
            <xsl:call-template name="ubl:invoice-type-code">
                <xsl:with-param name="code" select="$invoiceTypeCode" />
            </xsl:call-template>

            <!-- Invoice Notes -->
            <xsl:call-template name="ubl:notes-french-legal">
                <xsl:with-param name="AAB"
                    select="$CGV" />
            </xsl:call-template>

            <!-- Currency Code -->
            <xsl:call-template name="ubl:currency-code">
                <xsl:with-param name="code" select="$currencyCode" />
            </xsl:call-template>

            <!-- Contract reference -->
            <xsl:call-template name="ubl:contract-id">
                <xsl:with-param name="contractId" select="$contractID" />
            </xsl:call-template>

            <!-- Supplier -->
            <xsl:variable
                name="supplierName"
                select="$invoiceGroup/*[local-name()='AddrReglementAlphaName']" />
            <xsl:variable
                name="supplierStreet"
                select="$invoiceGroup/*[local-name()='AddrReglementLine1']" />
            <xsl:variable
                name="supplierCity"
                select="$invoiceGroup/*[local-name()='AddrReglementVille']" />
            <xsl:variable
                name="supplierPostalCode"
                select="$invoiceGroup/*[local-name()='AddrReglementPostalCode']" />
            <xsl:variable
                name="supplierCountry"
                select="$invoiceGroup/*[local-name()='Papillon_Code_Pays_ID106']" />
            <!-- TO BE DEFINED -->
            <xsl:variable
                name="supplierGln"
                select="''" />
            <!-- TO BE DEFINED -->
            <xsl:variable
                name="supplierSiren"
                select="''" />
            <!-- TO BE DEFINED -->
            <xsl:variable
                name="supplierVat"
                select="''" />
            <cac:AccountingSupplierParty>
                <cac:Party>
                    <!-- Endpoint -->
                    <xsl:call-template name="ubl:endpoint-id">
                        <xsl:with-param name="id" select="$endPointID" />
                        <xsl:with-param name="missingMarker" select="'**MISSING_BR-FR-23**'" />
                    </xsl:call-template>

                    <!-- GLN -->
                    <xsl:call-template name="ubl:party-gln">
                        <xsl:with-param name="gln" select="$supplierGln" />
                        <xsl:with-param name="missingMarker" select="'**MISSING_BR-FR-10**'" />
                    </xsl:call-template>

                    <!-- SIREN -->
                    <xsl:call-template name="ubl:party-siren">
                        <xsl:with-param name="siren" select="$supplierSiren" />
                        <xsl:with-param name="missingMarker" select="'**MISSING_BR-FR-10**'" />
                    </xsl:call-template>

                    <!-- PartyName (no empty Name) -->
                    <xsl:call-template name="ubl:party-name">
                        <xsl:with-param name="partyName" select="$supplierName" />
                    </xsl:call-template>

                    <!-- PostalAddress (only if at least one field exists) -->
                    <xsl:call-template name="ubl:address">
                        <xsl:with-param name="qname" select="'cac:PostalAddress'" />
                        <xsl:with-param name="street" select="$supplierStreet" />
                        <xsl:with-param name="city" select="$supplierCity" />
                        <xsl:with-param name="postalCode" select="$supplierPostalCode" />
                        <xsl:with-param name="countryCode" select="$supplierCountry" />
                        <xsl:with-param name="defaultCountry" select="'FR'" />
                    </xsl:call-template>

                    <!-- VAT -->
                    <xsl:call-template name="ubl:party-tax-vat">
                        <xsl:with-param name="vat" select="$supplierVat" />
                        <xsl:with-param name="missingMarker" select="'FR01572126043'" />
                    </xsl:call-template>

                    <!-- PartyLegalEntity -->
                    <cac:PartyLegalEntity>
                        <!-- supplier Name -->
                        <cbc:RegistrationName>
                            <xsl:value-of select="$supplierName" />
                        </cbc:RegistrationName>

                        <!-- supplier SIREN -->
                        <xsl:call-template name="ubl:company-siren">
                            <xsl:with-param name="siren" select="$supplierSiren" />
                            <xsl:with-param name="missingMarker" select="'572126043'" />
                        </xsl:call-template>

                        <!-- supplier Legal Form -->
                        <cbc:CompanyLegalForm>
                            <xsl:value-of
                                select="$invoiceGroup/*[local-name()='MessageInfoJuridiqueEtAdresseSociete']" />
                        </cbc:CompanyLegalForm>
                    </cac:PartyLegalEntity>

                    <!-- Contact -->
                    <xsl:variable name="contactName"
                        select="$invoiceGroup/*[local-name()='BureauComAddrAlphaName']" />
                    <xsl:variable name="contactTel"
                        select="$invoiceGroup/*[local-name()='MessageInfoServiceClientTel']" />
                    <xsl:variable name="contactMail"
                        select="$invoiceGroup/*[local-name()='MessageInfoServiceClientMail']" />

                    <xsl:call-template name="ubl:contact">
                        <xsl:with-param name="name" select="$contactName" />
                        <xsl:with-param name="telephone" select="$contactTel" />
                        <xsl:with-param name="electronicMail" select="$contactMail" />
                    </xsl:call-template>

                </cac:Party>
            </cac:AccountingSupplierParty>

            <!-- Customer -->
            <!-- TO BE DEFINED -->
            <xsl:variable
                name="customerName"
                select="$invoiceGroup/*[local-name()='SoldToAddrAlphaName']" />
            <xsl:variable
                name="customerSiren"
                select="''" />
            <xsl:variable name="cStreet"
                select="$invoiceGroup/*[local-name()='SoldToAddrLine1']" />
            <xsl:variable name="cStreet2"
                select="$invoiceGroup/*[local-name()='SoldToAddrLine2']" />
            <xsl:variable name="cStreet3"
                select="$invoiceGroup/*[local-name()='SoldToAddrLine3']" />
            <xsl:variable name="cStreet4"
                select="$invoiceGroup/*[local-name()='SoldToAddrLine4']" />
            <xsl:variable name="cCity"
                select="$invoiceGroup/*[local-name()='SoldToAddrVille']" />
            <xsl:variable name="cPost"
                select="$invoiceGroup/*[local-name()='SoldToAddrCodePostal']" />
            <xsl:variable name="cCountryRaw"
                select="$invoiceGroup/*[local-name()='Sold_To_Code_Pays_ID31']" />

            <cac:AccountingCustomerParty>
                <cac:Party>
                    <xsl:call-template name="ubl:endpoint-id">
                        <xsl:with-param name="id" select="''" />
                    </xsl:call-template>

                    <!-- PostalAddress  -->
                    <xsl:call-template name="ubl:address">
                        <xsl:with-param name="qname" select="'cac:PostalAddress'" />
                        <xsl:with-param name="street" select="$cStreet" />
                        <xsl:with-param name="street2" select="$cStreet2" />
                        <xsl:with-param name="street3" select="$cStreet3" />
                        <xsl:with-param name="street4" select="$cStreet4" />
                        <xsl:with-param name="city" select="$cCity" />
                        <xsl:with-param name="postalCode" select="$cPost" />
                        <xsl:with-param name="countryCode" select="$cCountryRaw" />
                        <xsl:with-param name="defaultCountry" select="'FR'" />
                    </xsl:call-template>

                    <!-- PartyLegalEntity -->
                    <xsl:call-template name="ubl:party-legal-entity">
                        <xsl:with-param name="customerName" select="$customerName" />
                        <xsl:with-param name="customerSiren" select="$customerSiren" />
                    </xsl:call-template>

                </cac:Party>
            </cac:AccountingCustomerParty>

            <!-- Delivery -->
            <xsl:variable
                name="deliveryId"
                select="$shipToGroup/*[local-name()='CodeLivreALKY']" />
            <xsl:variable name="dStreet"
                select="$shipToGroup/*[local-name()='ShipToAddrLine1']" />
            <xsl:variable name="dStreet2"
                select="$shipToGroup/*[local-name()='ShipToAddrLine2']" />
            <xsl:variable name="dStreet3"
                select="$shipToGroup/*[local-name()='ShipToAddrLine3']" />
            <xsl:variable name="dStreet4"
                select="$shipToGroup/*[local-name()='ShipToAddrLine4']" />
            <xsl:variable name="dCity"
                select="$shipToGroup/*[local-name()='ShipToAddrVille']" />
            <xsl:variable name="dPostalCode"
                select="$shipToGroup/*[local-name()='ShipToAddrCodePostal']" />
            <xsl:variable name="dCountry"
                select="$shipToGroup/*[local-name()='Ship_To_Code_Pays_ID11']" />

            <xsl:if
                test="count($shipToGroup) = 1 and (ubl:is-not-empty($deliveryId) or ubl:is-not-empty($dStreet) or ubl:is-not-empty($dStreet2) or ubl:is-not-empty($dStreet3) or ubl:is-not-empty($dStreet4) or ubl:is-not-empty($dCity) or ubl:is-not-empty($dPostalCode) or ubl:is-not-empty($dCountry))">
                <cac:Delivery>
                    <cac:DeliveryLocation>
                        <xsl:call-template name="ubl:emit-scheme">
                            <xsl:with-param name="qname" select="'cbc:ID'" />
                            <xsl:with-param name="value" select="$deliveryId" />
                            <xsl:with-param name="code" select="'schemeID'" />
                            <xsl:with-param name="codeValue" select="'0190'" />
                        </xsl:call-template>

                        <!-- Delivery Address  -->
                        <xsl:call-template name="ubl:address">
                            <xsl:with-param name="qname" select="'cac:Address'" />
                            <xsl:with-param name="street" select="$dStreet" />
                            <xsl:with-param name="street2" select="$dStreet2" />
                            <xsl:with-param name="street3" select="$dStreet3" />
                            <xsl:with-param name="street4" select="$dStreet4" />
                            <xsl:with-param name="city" select="$dCity" />
                            <xsl:with-param name="postalCode" select="$dPostalCode" />
                            <xsl:with-param name="countryCode" select="$dCountry" />
                            <xsl:with-param name="defaultCountry" select="'FR'" />
                        </xsl:call-template>

                    </cac:DeliveryLocation>
                </cac:Delivery>
            </xsl:if>

            <!-- PaymentMeans / Terms -->
            <xsl:variable
                name="paymentCode"
                select="ubl:payment-code($invoiceGroup/*[local-name()='CodeModePaiement_ID133'])" />
            <xsl:variable
                name="iban"
                select="$invoiceGroup/*[local-name()='CodeIBANBurCom']" />
            <xsl:variable
                name="swift"
                select="$invoiceGroup/*[local-name()='CodeBICBurCom']" />
            <xsl:variable
                name="paymentTerms"
                select="$invoiceGroup/*[local-name()='DescriptionModePaiement']" />

            <xsl:call-template name="ubl:payment-means">
                <xsl:with-param name="paymentCode" select="$paymentCode" />
                <xsl:with-param name="iban" select="$iban" />
                <xsl:with-param name="swift" select="$swift" />
                <xsl:with-param name="supplierName" select="$supplierName" />
            </xsl:call-template>

            <xsl:if test="ubl:is-not-empty($paymentTerms)">
                <cac:PaymentTerms>
                    <xsl:call-template name="ubl:emit">
                        <xsl:with-param name="qname" select="'cbc:Note'" />
                        <xsl:with-param name="value" select="$paymentTerms" />
                    </xsl:call-template>
                </cac:PaymentTerms>
            </xsl:if>

            <!-- TaxTotal (using ubl:tax-subtotal template) -->
            <cac:TaxTotal>
                <xsl:variable
                    name="taxAmount"
                    select="$invoiceGroup/*[local-name()='MontantTotalTVA']" />
                <xsl:variable
                    name="vatGroup"
                    select="$invoiceGroup/*[local-name()='LigneRecapTVA']" />
                <xsl:variable
                    name="taxCurrency"
                    select="$defaultCurrency" />

                <xsl:call-template name="ubl:emit-scheme">
                    <xsl:with-param name="qname" select="'cbc:TaxAmount'" />
                    <xsl:with-param name="value" select="$taxAmount" />
                    <xsl:with-param name="code" select="'currencyID'" />
                    <xsl:with-param name="codeValue" select="$taxCurrency" />
                </xsl:call-template>

                <xsl:for-each select="$vatGroup">
                    <xsl:variable
                        name="taxableLineAmount"
                        select="*[local-name()='MontantHTAvecTVA']" />
                    <xsl:variable
                        name="taxLineAmount"
                        select="*[local-name()='MontantLigneTVA']" />
                    <xsl:variable
                        name="taxLineRate"
                        select="*[local-name()='TauxTVA']" />
                    <!-- TO BE DEFINED -->
                    <xsl:variable
                        name="taxLineID"
                        select="'S'" />
                    <!-- TO BE DEFINED -->
                    <xsl:variable
                        name="taxLineCurrency"
                        select="$defaultCurrency" />

                    <xsl:call-template
                        name="ubl:tax-subtotal">
                        <xsl:with-param name="taxableAmount" select="$taxableLineAmount" />
                        <xsl:with-param name="taxAmount" select="$taxLineAmount" />
                        <xsl:with-param name="taxRate" select="$taxLineRate" />
                        <xsl:with-param name="taxID" select="$taxLineID" />
                        <xsl:with-param name="currencyID" select="$taxLineCurrency" />
                        <xsl:with-param name="invalidRateMarker"
                            select="'**INVALID_BT-152_TVA_RATE**'" />
                    </xsl:call-template>
                </xsl:for-each>
            </cac:TaxTotal>

            <!-- Monetary totals -->
            <xsl:variable
                name="totalWithoutVAT"
                select="$invoiceGroup/*[local-name()='TotalMontantHT']" />
            <xsl:variable
                name="totalWithVAT"
                select="$invoiceGroup/*[local-name()='MontantTTC']" />
            <xsl:variable
                name="totalCurrencyCode"
                select="$defaultCurrency" />

            <xsl:call-template
                name="ubl:legal-monetary-total">
                <xsl:with-param name="totalWithoutVAT" select="$totalWithoutVAT" />
                <xsl:with-param name="totalWithVAT" select="$totalWithVAT" />
                <xsl:with-param name="currencyID" select="$totalCurrencyCode" />
            </xsl:call-template>

            <!-- Lines (no empty qty/percent/price) -->
            <xsl:for-each
                select="$shipToGroup/*[local-name()='Detail_ligne_article_S2']">

                <xsl:variable 
                    name="itemLabel"
                    select="normalize-space(*[local-name()='DescriptionLine1_ID278'])" />
                <xsl:variable
                    name="itemCode"
                    select="normalize-space(*[local-name()='ItemNoUnknownFormat_ID282'])" />
                <xsl:variable
                    name="deliveryDate"
                    select="normalize-space(*[local-name()='Date_Livraison_ID340'])" />
                <xsl:variable
                    name="bareme"
                    select="normalize-space(*[local-name()='szIntituleBareme_RMK3_ID324'])" />
                <xsl:variable
                    name="nbLitres"
                    select="ubl:normalize-quantity(*[local-name()='Nb_Litres_Livres_ID344'])" />
                <xsl:variable
                    name="temperature"
                    select="normalize-space(*[local-name()='Temperature_Livr_IR04_ID346'])" />
                <xsl:variable
                    name="masseVolumique"
                    select="ubl:normalize-quantity(*[local-name()='Masse_Vol_IR05_ID348'])" />
                <xsl:variable
                    name="lineQuantity"
                    select="ubl:normalize-quantity(*[local-name()='Poids_Quantité_Livrée_UORG_ID354'])" />
                <xsl:variable
                    name="UoM" 
                    select="normalize-space(*[local-name()='szDescUMShort_AA03_ID380'])" />
                <xsl:variable
                    name="lineAmount" 
                    select="*[local-name()='MntHT_ID356']" />
                <xsl:variable
                    name="unitPrice"
                    select="normalize-space(*[local-name()='Prix_PrixNet_UPRC_ID352'])" />
                <xsl:variable
                    name="discountAmount"
                    select="normalize-space(*[local-name()='Remise_exceptionnelle_S14']/*[local-name()='mnMontantUnitaireRemiseExcep_F_ID8'])" />
                <xsl:variable
                    name="itemTaxAmount"
                    select="ubl:normalize-percent(normalize-space(*[local-name()='AZ_Taux_de_TVA_ID250']))" />
                <xsl:variable 
                    name="unitCode" 
                    select="ubl:unit-code($UoM)" />
                <!-- TO BE DEFINED -->
                <xsl:variable
                    name="itemTaxType"
                    select="'S'" />

                <cac:InvoiceLine>
                    <cbc:ID>
                        <xsl:number level="any" count="*[local-name()='Detail_ligne_article_S2']"
                            format="1" />
                    </cbc:ID>

                    <!-- InvoicedQuantity: only if quantity exists; omit unitCode if empty -->
                    <xsl:call-template name="ubl:emit-scheme">
                            <xsl:with-param name="qname" select="'cbc:InvoicedQuantity'" />
                            <xsl:with-param name="value" select="$lineQuantity" />
                            <xsl:with-param name="code" select="'unitCode'" />
                            <xsl:with-param name="codeValue" select="$unitCode" />
                    </xsl:call-template>

                    <xsl:call-template name="ubl:emit-scheme">
                            <xsl:with-param name="qname" select="'cbc:LineExtensionAmount'" />
                            <xsl:with-param name="value" select="ubl:normalize-amount($lineAmount)" />
                            <xsl:with-param name="code" select="'currencyID'" />
                            <xsl:with-param name="codeValue" select="$defaultCurrency" />
                    </xsl:call-template>

                    <cac:Item>
                        <xsl:call-template name="ubl:emit">
                            <xsl:with-param name="qname" select="'cbc:Name'" />
                            <xsl:with-param name="value" select="$itemLabel" />
                        </xsl:call-template>

                        <xsl:call-template name="ubl:classified-tax-category">
                            <xsl:with-param name="id" select="$itemTaxType" />
                            <xsl:with-param name="percent" select="$itemTaxAmount" />
                        </xsl:call-template>

                        <xsl:if test="$itemCode = '3'">

                            <xsl:call-template name="ubl:additional-item-property">
                                <xsl:with-param name="name" select="'Livraison du'" />
                                <xsl:with-param name="value" select="$deliveryDate" />
                            </xsl:call-template>

                            <xsl:call-template name="ubl:additional-item-property">
                                <xsl:with-param name="name" select="'Barème'" />
                                <xsl:with-param name="value" select="$bareme" />
                            </xsl:call-template>

                            <xsl:if test="$nbLitres != ''">
                                <xsl:call-template name="ubl:additional-item-property">
                                    <xsl:with-param name="name" select="'Barème'" />
                                    <xsl:with-param name="value" select="concat($nbLitres, ' litres livrés à ', $temperature, '°C (masse volumique = ', $masseVolumique, ')')" />
                                </xsl:call-template>
                            </xsl:if>
                        </xsl:if>
                    </cac:Item>

                     <!-- PriceAmount: only if non-empty -->
                    <xsl:call-template name="ubl:line-price">
                        <xsl:with-param name="unitPrice" select="$unitPrice" />
                        <xsl:with-param name="unitCode" select="$unitCode" />
                        <xsl:with-param name="currencyCode" select="$defaultCurrency" />
                        <xsl:with-param name="discountAmount" select="$discountAmount" />
                    </xsl:call-template>

                </cac:InvoiceLine>
            </xsl:for-each>

        </Invoice>
    </xsl:template>

</xsl:stylesheet>