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
            select="/*/*[local-name()='facture'] | /*[local-name()='facture']" />
        <xsl:variable
            name="invoiceMain"
            select="$invoiceGroup/*[local-name()='facturePrincipale']" />
        <xsl:variable
            name="enrichment"
            select="$invoiceMain/*[local-name()='enrichissementUBL']" />
        <xsl:variable
            name="documentNumber"
            select="$invoiceGroup/*[local-name()='numFact']" />   
        <xsl:variable
            name="issueDate"
            select="$invoiceGroup/*[local-name()='dateConstitution']" />   
        <xsl:variable
            name="dueDate"
            select="$enrichment/*[local-name()='dateEcheance']" />
        <!-- TO BE DEFINED -->
        <xsl:variable
            name="invoiceTypeCode"
            select="''" />
        <xsl:variable name="CGV"
            select="string-join($invoiceMain/*[local-name()='CGV']/*[local-name()='libelleSection']/*[local-name()='libelle'], ' ')" />
        <xsl:variable
            name="currencyCode"
            select="''" />
        <xsl:variable
            name="contractID"
            select="$invoiceMain/*[local-name()='referenceClient']/*[local-name()='codeClient']" />
        <!-- TO BE DEFINED -->
        <xsl:variable
            name="endPointID"
            select="''" />
        <xsl:variable
            name="legalForm"
            select="concat($enrichment/*[local-name()='siege'], ' - ', $invoiceGroup/*[local-name()='CapitalSocial'], ' - ', $invoiceGroup/*[local-name()='CodeNaf'])" />

        <!-- END OF TOP-LEVEL VARIABLES -->

        <Invoice>
            <!-- Header -->
            <!-- BT-23: Business Process Type, BT-24: Specification Identifier -->
            <xsl:call-template name="ubl:invoice-header">
                <xsl:with-param name="profileID" select="'S1'" />
            </xsl:call-template>

            <!-- Identifiers -->
            <!-- BT-1: Invoice number -->
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:ID'" />
                <xsl:with-param name="value" select="$documentNumber" />
            </xsl:call-template>

            <!-- Issue Date -->
            <!-- BT-2: Invoice issue date -->
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:IssueDate'" />
                <xsl:with-param name="value" select="$issueDate" />
            </xsl:call-template>

            <!-- Due Date -->
            <!-- BT-9: Payment due date -->
            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:DueDate'" />
                <xsl:with-param name="value" select="$dueDate" />
            </xsl:call-template>

            <!-- Invoice Type Code -->
            <!-- BT-3: Invoice type code -->
            <xsl:call-template name="ubl:invoice-type-code">
                <xsl:with-param name="code" select="$invoiceTypeCode" />
            </xsl:call-template>

            <!-- Invoice Notes - Custom for ISC -->
            <!-- BT-22: Invoice note -->


            <xsl:call-template name="ubl:emit">
                <xsl:with-param name="qname" select="'cbc:Note'" />
                <xsl:with-param name="value"
                    select="concat('#SAF#', $enrichment/*[local-name()='urgence'])" />
            </xsl:call-template>

            <!-- BT-22: Invoice note (French legal requirements) -->
            <xsl:call-template name="ubl:notes-french-legal">
                <xsl:with-param name="AAB"
                    select="$CGV" />
            </xsl:call-template>

            <!-- Price History Notes -->
            <!-- BT-22: Invoice note (price history) -->
            <xsl:for-each
                select="$invoiceMain/*[local-name()='historiquePrix']/*[local-name()='periode']">
                <xsl:variable name="priceNote"
                    select="concat('#PRI#', *[local-name()='colonne1'], 
                            ' | Bareme: ', *[local-name()='colonne2'], 
                            ' | Prix: ', *[local-name()='colonne3'], 
                            ' | Remise: ', *[local-name()='colonne4'], 
                            ' | Facture: ', *[local-name()='colonne5'])" />
                <xsl:call-template
                    name="ubl:emit">
                    <xsl:with-param name="qname" select="'cbc:Note'" />
                    <xsl:with-param name="value" select="$priceNote" />
                </xsl:call-template>
            </xsl:for-each>

            <!-- Currency Code -->
            <!-- BT-5: Invoice currency code -->
            <xsl:call-template name="ubl:currency-code">
                <xsl:with-param name="code" select="$currencyCode" />
            </xsl:call-template>

            <!-- Contract reference -->
            <!-- BT-12: Contract reference -->
            <xsl:call-template name="ubl:contract-id">
                <xsl:with-param name="contractId" select="$contractID" />
            </xsl:call-template>

            <!-- Supplier (Vendeur) -->
            <xsl:variable
                name="supplierName"
                select="$enrichment/*[local-name()='Vendeur']/*[local-name()='name']" />
            <xsl:variable
                name="supplierStreet"
                select="$enrichment/*[local-name()='Vendeur']/*[local-name()='street']" />
            <xsl:variable
                name="supplierCity"
                select="$enrichment/*[local-name()='Vendeur']/*[local-name()='city']" />
            <xsl:variable
                name="supplierPostalCode"
                select="$enrichment/*[local-name()='Vendeur']/*[local-name()='postal']" />
            <xsl:variable
                name="supplierCountry"
                select="$enrichment/*[local-name()='Vendeur']/*[local-name()='country']" />
            <xsl:variable
                name="supplierGln"
                select="$enrichment/*[local-name()='Vendeur']/*[local-name()='GLN']" />
            <xsl:variable
                name="supplierSiren"
                select="$enrichment/*[local-name()='Vendeur']/*[local-name()='SIREN']" />
            <xsl:variable
                name="supplierVat"
                select="normalize-space(substring-after($invoiceGroup/*[local-name()='NumTVA'], 'TVA '))" />
            <xsl:variable
                name="supplierRCS"
                select="$invoiceGroup/*[local-name()='NumRCS']" />

            <!-- BG-4: Seller -->
            <cac:AccountingSupplierParty>
                <cac:Party>
                    <!-- Endpoint -->
                    <!-- BT-34: Seller electronic address -->
                    <xsl:call-template name="ubl:endpoint-id">
                        <xsl:with-param name="id" select="$endPointID" />
                        <xsl:with-param name="missingMarker" select="'**MISSING_BR-FR-23**'" />
                    </xsl:call-template>

                    <!-- GLN -->
                    <!-- BT-29: Seller identifier -->
                    <xsl:call-template name="ubl:party-gln">
                        <xsl:with-param name="gln" select="$supplierGln" />
                        <xsl:with-param name="missingMarker" select="'**MISSING_BR-FR-10**'" />
                    </xsl:call-template>

                    <!-- SIREN -->
                    <!-- BT-29: Seller identifier -->
                    <xsl:call-template name="ubl:party-siren">
                        <xsl:with-param name="siren" select="$supplierSiren" />
                        <xsl:with-param name="missingMarker" select="'**MISSING_BR-FR-10**'" />
                    </xsl:call-template>

                    <!-- PartyName -->
                    <!-- BT-28: Seller trading name -->
                    <xsl:call-template name="ubl:party-name">
                        <xsl:with-param name="partyName" select="$supplierName" />
                    </xsl:call-template>

                    <!-- PostalAddress -->
                    <!-- BG-5: Seller postal address -->
                    <xsl:call-template name="ubl:address">
                        <xsl:with-param name="qname" select="'cac:PostalAddress'" />
                        <xsl:with-param name="street" select="$supplierStreet" />
                        <xsl:with-param name="city" select="$supplierCity" />
                        <xsl:with-param name="postalCode" select="$supplierPostalCode" />
                        <xsl:with-param name="countryCode" select="$supplierCountry" />
                        <xsl:with-param name="defaultCountry" select="'FR'" />
                    </xsl:call-template>

                    <!-- VAT -->
                    <!-- BT-31: Seller VAT identifier -->
                    <xsl:call-template name="ubl:party-tax-vat">
                        <xsl:with-param name="vat" select="$supplierVat" />
                        <xsl:with-param name="missingMarker" select="'**MISSING_BT-31_VAT**'" />
                    </xsl:call-template>

                    <!-- PartyLegalEntity -->
                    <cac:PartyLegalEntity>
                        <!-- BT-27: Seller name -->
                        <cbc:RegistrationName>
                            <xsl:value-of select="$supplierName" />
                        </cbc:RegistrationName>

                        <!-- BT-30: Seller legal registration identifier -->
                        <xsl:call-template name="ubl:company-siren">
                            <xsl:with-param name="siren" select="$supplierSiren" />
                            <xsl:with-param name="missingMarker" select="'**MISSING_BT-30_SIREN**'" />
                        </xsl:call-template>

                        <!-- supplier Legal Form -->
                        <!-- BT-33: Seller additional legal information -->
                        <xsl:call-template name="ubl:emit">
                            <xsl:with-param name="qname" select="'cbc:CompanyLegalForm'" />
                            <xsl:with-param name="value" select="$legalForm" />
                        </xsl:call-template>
                    </cac:PartyLegalEntity>

                    <!-- Contact -->
                    <xsl:variable name="contactName"
                        select="$enrichment/*[local-name()='Vendeur']/*[local-name()='contact']/*[local-name()='name']" />
                    <xsl:variable name="contactTel"
                        select="$enrichment/*[local-name()='Vendeur']/*[local-name()='contact']/*[local-name()='phone']" />
                    <xsl:variable name="contactMail"
                        select="$enrichment/*[local-name()='Vendeur']/*[local-name()='contact']/*[local-name()='email']" />

                    <!-- BG-6: Seller contact -->
                    <xsl:call-template name="ubl:contact">
                        <xsl:with-param name="name" select="$contactName" />
                        <xsl:with-param name="telephone" select="$contactTel" />
                        <xsl:with-param name="electronicMail" select="$contactMail" />
                    </xsl:call-template>

                </cac:Party>
            </cac:AccountingSupplierParty>

            <!-- Customer -->
            <xsl:variable
                name="billingCustomer"
                select="$invoiceMain/*[local-name()='clientFacturation']" />
            <xsl:variable
                name="customerName"
                select="$billingCustomer/*[local-name()='factIdentite']" />
            <xsl:variable
                name="customerSiren"
                select="$enrichment/*[local-name()='Acheteur']/*[local-name()='SIREN']" />
            <xsl:variable name="cStreet"
                select="$billingCustomer/*[local-name()='adresse'][1]/*[local-name()='libelle']" />
            <xsl:variable name="cStreet2"
                select="$billingCustomer/*[local-name()='adresse'][2]/*[local-name()='libelle']" />
            <xsl:variable name="cCityPostal"
                select="$billingCustomer/*[local-name()='cPVille']" />
            <xsl:variable name="cCity"
                select="ubl:city-name($cCityPostal)" />
            <xsl:variable name="cPostalCode"
                select="ubl:postal-code($cCityPostal)" />
            <xsl:variable name="cCountryRaw"
                select="$billingCustomer/*[local-name()='pays']" />

            <!-- BG-7: Buyer -->
            <cac:AccountingCustomerParty>
                <cac:Party>
                    <!-- BT-49: Buyer electronic address -->
                    <xsl:call-template name="ubl:endpoint-id">
                        <xsl:with-param name="id" select="''" />
                    </xsl:call-template>

                    <!-- PostalAddress -->
                    <!-- BG-8: Buyer postal address -->
                    <xsl:call-template name="ubl:address">
                        <xsl:with-param name="qname" select="'cac:PostalAddress'" />
                        <xsl:with-param name="street" select="$cStreet" />
                        <xsl:with-param name="street2" select="$cStreet2" />
                        <xsl:with-param name="city" select="$cCity" />
                        <xsl:with-param name="postalCode" select="$cPostalCode" />
                        <xsl:with-param name="countryCode" select="$cCountryRaw" />
                        <xsl:with-param name="defaultCountry" select="'FR'" />
                    </xsl:call-template>

                    <!-- PartyLegalEntity -->
                    <!-- BT-44: Buyer name -->
                    <xsl:call-template name="ubl:party-legal-entity">
                        <xsl:with-param name="customerName" select="$customerName" />
                        <xsl:with-param name="customerSiren" select="$customerSiren" />
                    </xsl:call-template>

                    <!-- Agent Party (Concédant) if exists -->
                    <xsl:variable name="agentName"
                        select="$enrichment/*[local-name()='agent']/*[local-name()='name']" />
                    <xsl:variable name="agentStreet"
                        select="$enrichment/*[local-name()='agent']/*[local-name()='street']" />
                    <xsl:variable name="agentCity"
                        select="$enrichment/*[local-name()='agent']/*[local-name()='city']" />
                    <xsl:variable name="agentPostal"
                        select="$enrichment/*[local-name()='agent']/*[local-name()='postal']" />
                    <xsl:variable name="agentCountry"
                        select="$enrichment/*[local-name()='agent']/*[local-name()='country']" />

                    <xsl:if test="ubl:is-not-empty($agentName)">
                        <cac:AgentParty>
                            <xsl:call-template name="ubl:party-name">
                                <xsl:with-param name="partyName" select="$agentName" />
                            </xsl:call-template>

                            <xsl:call-template name="ubl:address">
                                <xsl:with-param name="qname" select="'cac:PostalAddress'" />
                                <xsl:with-param name="street" select="$agentStreet" />
                                <xsl:with-param name="city" select="$agentCity" />
                                <xsl:with-param name="postalCode" select="$agentPostal" />
                                <xsl:with-param name="countryCode" select="$agentCountry" />
                                <xsl:with-param name="defaultCountry" select="'FR'" />
                            </xsl:call-template>
                        </cac:AgentParty>
                    </xsl:if>

                </cac:Party>
            </cac:AccountingCustomerParty>

            <!-- Delivery -->
            <xsl:variable
                name="deliveryCustomer"
                select="$invoiceMain/*[local-name()='clientConsommation']" />
            <xsl:variable
                name="deliveryId"
                select="$deliveryCustomer/*[local-name()='refLogement']" />
            <xsl:variable name="dStreet"
                select="$deliveryCustomer/*[local-name()='adresse']/*[local-name()='libelle']" />
            <xsl:variable name="dCityPostal"
                select="$deliveryCustomer/*[local-name()='cPVille']" />
            <xsl:variable name="dCity"
                select="ubl:city-name($dCityPostal)" />
            <xsl:variable name="dPostalCode"
                select="ubl:postal-code($dCityPostal)" />
            <xsl:variable name="dCountry"
                select="$deliveryCustomer/*[local-name()='country']" />

            <!-- BG-13: Delivery information -->
            <xsl:if
                test="ubl:is-not-empty($deliveryId) or ubl:is-not-empty($dStreet) or ubl:is-not-empty($dCity) or ubl:is-not-empty($dPostalCode) or ubl:is-not-empty($dCountry)">
                <cac:Delivery>
                    <cac:DeliveryLocation>
                        <!-- BT-71: Deliver to location identifier -->
                        <xsl:call-template name="ubl:emit-scheme">
                            <xsl:with-param name="qname" select="'cbc:ID'" />
                            <xsl:with-param name="value" select="$deliveryId" />
                            <xsl:with-param name="code" select="'schemeID'" />
                            <xsl:with-param name="codeValue" select="'0190'" />
                        </xsl:call-template>

                        <!-- Delivery Address -->
                        <!-- BG-15: Deliver to address -->
                        <xsl:call-template name="ubl:address">
                            <xsl:with-param name="qname" select="'cac:Address'" />
                            <xsl:with-param name="street" select="$dStreet" />
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
                select="ubl:payment-code($enrichment/*[local-name()='Paiement']/*[local-name()='Code'])" />
            <xsl:variable
                name="iban"
                select="$enrichment/*[local-name()='Paiement']/*[local-name()='IBAN']" />
            <xsl:variable
                name="swift"
                select="$enrichment/*[local-name()='Paiement']/*[local-name()='BIC']" />
            <xsl:variable
                name="paymentAccountName"
                select="$enrichment/*[local-name()='Paiement']/*[local-name()='Titulaire']" />
            <xsl:variable
                name="paymentTerms"
                select="$enrichment/*[local-name()='Paiement']/*[local-name()='Condition']" />

            <!-- BG-16: Payment instructions -->
            <xsl:call-template name="ubl:payment-means">
                <xsl:with-param name="paymentCode" select="$paymentCode" />
                <xsl:with-param name="iban" select="$iban" />
                <xsl:with-param name="swift" select="$swift" />
                <xsl:with-param name="supplierName" select="$paymentAccountName" />
            </xsl:call-template>

            <!-- BT-20: Payment terms -->
            <xsl:if test="ubl:is-not-empty($paymentTerms)">
                <cac:PaymentTerms>
                    <xsl:call-template name="ubl:emit">
                        <xsl:with-param name="qname" select="'cbc:Note'" />
                        <xsl:with-param name="value" select="$paymentTerms" />
                    </xsl:call-template>
                </cac:PaymentTerms>
            </xsl:if>

            <!-- TaxTotal -->
            <xsl:variable
                name="amountDetails"
                select="$invoiceMain/*[local-name()='detailMontants']" />
            <xsl:variable
                name="finalAmounts"
                select="$invoiceMain/*[local-name()='montantsFinaux']" />

            <!-- BG-23: VAT breakdown -->
            <cac:TaxTotal>
                <xsl:variable
                    name="taxAmount"
                    select="ubl:normalize-amount($amountDetails/*[local-name()='montantTVAGlobale'])" />
                <xsl:variable
                    name="vatGroup"
                    select="$amountDetails/*[local-name()='TVA']" />
                <xsl:variable
                    name="taxCurrency"
                    select="$defaultCurrency" />

                <!-- BT-110: Invoice total VAT amount -->
                <xsl:call-template name="ubl:emit-scheme">
                    <xsl:with-param name="qname" select="'cbc:TaxAmount'" />
                    <xsl:with-param name="value" select="$taxAmount" />
                    <xsl:with-param name="code" select="'currencyID'" />
                    <xsl:with-param name="codeValue" select="$taxCurrency" />
                </xsl:call-template>

                <xsl:for-each select="$vatGroup">
                    <xsl:variable
                        name="taxableLineAmount"
                        select="*[local-name()='quantiteTauxTVA']" />
                    <xsl:variable
                        name="taxLineAmount"
                        select="*[local-name()='montantTauxTVA']" />
                    <xsl:variable
                        name="taxLineRate"
                        select="ubl:extract-tax-rate(*[local-name()='libelleTauxTVA'])" /> 
                    <xsl:variable
                        name="taxLineID"
                        select="'S'" />
                    <xsl:variable
                        name="taxLineCurrency"
                        select="$defaultCurrency" />

                    <xsl:call-template name="ubl:tax-subtotal">
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
                select="$finalAmounts/*[local-name()='montantHT']" />
            <xsl:variable
                name="totalWithVAT"
                select="$amountDetails/*[local-name()='montantTTCTotal']" />
            <xsl:variable
                name="totalCurrencyCode"
                select="$defaultCurrency" />

            <!-- BG-22: Document totals -->
            <xsl:call-template name="ubl:legal-monetary-total">
                <xsl:with-param name="totalWithoutVAT" select="$totalWithoutVAT" />
                <xsl:with-param name="totalWithVAT" select="$totalWithVAT" />
                <xsl:with-param name="currencyID" select="$totalCurrencyCode" />
            </xsl:call-template>

            <!-- Invoice Lines -->
            <xsl:for-each select="$invoiceMain/*[local-name()='famillePrestation']">
                <xsl:for-each select="*[local-name()='article']">

                    <xsl:variable
                        name="libelle"
                        select="*[local-name()='libelleArticle']" />
                    <xsl:variable
                        name="dateDebut"
                        select="*[local-name()='DateDebut']" />
                    <xsl:variable
                        name="dateFin"
                        select="*[local-name()='DateFin']" />
                    <xsl:variable
                        name="indexInitial"
                        select="*[local-name()='indexInitial']" />
                    <xsl:variable
                        name="indexFinal"
                        select="*[local-name()='indexFinal']" />
                    <xsl:variable
                        name="differenceIndex"
                        select="*[local-name()='differenceIndex']" />
                    <xsl:variable
                        name="coeffThermique"
                        select="*[local-name()='coeffThermique']" />
                    <xsl:variable
                        name="consokWh"
                        select="*[local-name()='consokWh']" />
                    <xsl:variable
                        name="noteREG"
                        select="*[local-name()='noteREG']" />
                    <xsl:variable
                        name="UM"
                        select="*[local-name()='UM']" />
                    <xsl:variable
                        name="referenceCompteur"
                        select="*[local-name()='referenceCompteur']" />
                    <xsl:variable
                        name="montantHT"
                        select="*[local-name()='montantHT']" />
                    <xsl:variable
                        name="unitPrice"
                        select="*[local-name()='prixUnitairekWh']" />
                    <xsl:variable
                        name="tauxTaxe"
                        select="ubl:normalize-percent(*[local-name()='tauxTaxe'])" />
                    <xsl:variable
                        name="unitCode"
                        select="$UM" />
                    <!-- TO BE DEFINED -->
                    <xsl:variable
                        name="itemTaxType"
                        select="'S'" />

                    <xsl:if
                        test="ubl:is-not-empty($libelle) or ubl:is-not-empty($montantHT)">
                        <!-- BG-25: Invoice line -->
                        <cac:InvoiceLine>
                            <!-- BT-126: Invoice line identifier -->
                            <cbc:ID>
                                <xsl:number level="any" count="*[local-name()='article']" format="1" />
                            </cbc:ID>

                            <!-- Note REG -->
                            <!-- BT-127: Invoice line note -->
                            <xsl:if test="ubl:is-not-empty($noteREG)">
                                <xsl:call-template name="ubl:emit">
                                    <xsl:with-param name="qname" select="'cbc:Note'" />
                                    <xsl:with-param name="value" select="concat('#REG#', $noteREG)" />
                                </xsl:call-template>
                            </xsl:if>

                            <!-- InvoicedQuantity -->
                            <xsl:variable name="quantityValue">
                                <xsl:choose>
                                    <xsl:when test="$UM = 'KWH' and ubl:is-not-empty($consokWh)">
                                        <xsl:value-of select="ubl:normalize-quantity($consokWh)" />
                                    </xsl:when>
                                    <xsl:otherwise>1</xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>

                            <!-- BT-129: Invoiced quantity -->
                            <xsl:call-template name="ubl:emit-scheme">
                                <xsl:with-param name="qname" select="'cbc:InvoicedQuantity'" />
                                <xsl:with-param name="value" select="$quantityValue" />
                                <xsl:with-param name="code" select="'unitCode'" />
                                <xsl:with-param name="codeValue" select="$unitCode" />
                            </xsl:call-template>

                            <!-- BT-131: Invoice line net amount -->
                            <xsl:call-template name="ubl:emit-scheme">
                                <xsl:with-param name="qname" select="'cbc:LineExtensionAmount'" />
                                <xsl:with-param name="value"
                                    select="ubl:normalize-amount($montantHT)" />
                                <xsl:with-param name="code" select="'currencyID'" />
                                <xsl:with-param name="codeValue" select="$defaultCurrency" />
                            </xsl:call-template>

                            <!-- Invoice Period -->
                            <!-- BG-26: Invoice line period -->
                            <xsl:if
                                test="ubl:is-not-empty($dateDebut) or ubl:is-not-empty($dateFin)">
                                <cac:InvoicePeriod>
                                    <!-- BT-134: Invoice line period start date -->
                                    <xsl:call-template name="ubl:emit">
                                        <xsl:with-param name="qname" select="'cbc:StartDate'" />
                                        <xsl:with-param name="value" select="$dateDebut" />
                                    </xsl:call-template>
                                    <!-- BT-135: Invoice line period end date -->
                                    <xsl:call-template name="ubl:emit">
                                        <xsl:with-param name="qname" select="'cbc:EndDate'" />
                                        <xsl:with-param name="value" select="$dateFin" />
                                    </xsl:call-template>
                                </cac:InvoicePeriod>
                            </xsl:if>

                            <cac:Item>
                                <!-- BT-153: Item name -->
                                <xsl:call-template name="ubl:emit">
                                    <xsl:with-param name="qname" select="'cbc:Name'" />
                                    <xsl:with-param name="value" select="$libelle" />
                                </xsl:call-template>

                                <!-- BT-151: Invoiced item VAT category code -->
                                <xsl:call-template name="ubl:classified-tax-category">
                                    <xsl:with-param name="id" select="$itemTaxType" />
                                    <xsl:with-param name="percent" select="$tauxTaxe" />
                                </xsl:call-template>

                                <!-- Additional Item Properties -->
                                <!-- BG-32: Item attributes -->
                                <xsl:call-template name="ubl:additional-item-property">
                                    <xsl:with-param name="name" select="'Ancien index'" />
                                    <xsl:with-param name="value" select="$indexInitial" />
                                </xsl:call-template>

                                <xsl:call-template name="ubl:additional-item-property">
                                    <xsl:with-param name="name" select="'Nouvel index'" />
                                    <xsl:with-param name="value" select="$indexFinal" />
                                </xsl:call-template>

                                <xsl:call-template name="ubl:additional-item-property">
                                    <xsl:with-param name="name" select="'Différence Index'" />
                                    <xsl:with-param name="value" select="$differenceIndex" />
                                </xsl:call-template>

                                <xsl:call-template name="ubl:additional-item-property">
                                    <xsl:with-param name="name" select="'Coefficient thermique'" />
                                    <xsl:with-param name="value" select="$coeffThermique" />
                                </xsl:call-template>

                                <xsl:call-template name="ubl:additional-item-property">
                                    <xsl:with-param name="name" select="'Référence compteur'" />
                                    <xsl:with-param name="value" select="$referenceCompteur" />
                                </xsl:call-template>

                            </cac:Item>

                            <!-- PriceAmount: only if non-empty -->
                            <!-- BG-29: Price details -->
                            <xsl:call-template name="ubl:line-price">
                                <xsl:with-param name="unitPrice" select="$unitPrice" />
                                <xsl:with-param name="unitCode" select="$unitCode" />
                                <xsl:with-param name="currencyCode" select="$defaultCurrency" />
                            </xsl:call-template>

                        </cac:InvoiceLine>
                    </xsl:if>
                </xsl:for-each>
            </xsl:for-each>

        </Invoice>
    </xsl:template>

</xsl:stylesheet>