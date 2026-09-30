package com.example.model

enum class TransformationPhase(
    val phaseNumber: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val fragmentDispersion: Float,
    val glowIntensity: Float
) {
    PHASE_01(
        1,
        "THE MONOLITH",
        "Physical futuristic sculpture",
        "The Z stands as a solid obsidian chrome monolith with subtle silver reflections, sharp glass-like facets, and soft volumetric light.",
        0.0f,
        0.3f
    ),
    PHASE_02(
        2,
        "GEOMETRIC FRACTURE",
        "Structural lattice division",
        "The monolithic form fractures along crystalline fault lines into hundreds of microscopic geometric polygons.",
        0.35f,
        0.55f
    ),
    PHASE_03(
        3,
        "ORBITAL DISPERSION",
        "Levitating particulate cloud",
        "Kinetic energy suspends the fragments outward in an anti-gravity computational field responding to user inertia.",
        0.75f,
        0.7f
    ),
    PHASE_04(
        4,
        "MORPHIC REORGANIZATION",
        "Assembly into 'ZAMA AI'",
        "The scattered particles converge, snapping magnetically into structural typographic architecture spelling ZAMA AI.",
        0.4f,
        0.85f
    ),
    PHASE_05(
        5,
        "LUMINESCENT EDIFICE",
        "Glowing architectural grid",
        "The typography activates as a radiant computational structure with cybernetic luminescence and volumetric photon paths.",
        0.15f,
        1.0f
    ),
    PHASE_06(
        6,
        "QUANTUM DISSOLUTION",
        "Transition into the stream",
        "The structure dissolves into fine computational stardust, flowing seamlessly into the underlying neural intelligence.",
        0.95f,
        0.9f
    )
}

enum class MaterialType(
    val title: String,
    val subtitle: String,
    val materialName: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val reflectionType: String,
    val description: String
) {
    TRUST(
        "TRUST",
        "Impervious Integrity",
        "Polished Black Chrome",
        0xFF1A1D24,
        0xFFE2E8F0,
        "Specular Mirror Raytracing",
        "A flawless, mirror-polished obsidian chrome surface reflecting zero-knowledge verifications with mathematical certainty."
    ),
    PRIVACY(
        "PRIVACY",
        "Zero Information Leakage",
        "Prismatic Transparent Glass",
        0xFF0E2238,
        0xFF38BDF8,
        "Refractive Index 1.52",
        "Crystalline optical glass that bends light while fully obscuring underlying customer data from third-party observation."
    ),
    COMPUTE(
        "COMPUTE",
        "Encrypted Execution",
        "Dark Metallic Micro-Structure",
        0xFF12131A,
        0xFF818CF8,
        "Anisotropic Carbon Weave",
        "High-density titanium-carbon chassis executing autonomous decisions directly over encrypted state machines."
    ),
    FREEDOM(
        "FREEDOM",
        "Autonomous Agency",
        "Luminous Photon Particles",
        0xFF022C22,
        0xFF34D399,
        "Volumetric Light Diffusion",
        "Weightless light filaments and dynamic particle vectors unchaining business owners from perpetual manual replies."
    )
}

data class TechStep(
    val number: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val formulaOrSpec: String,
    val status: String
)

val TECH_TIMELINE_STEPS = listOf(
    TechStep(
        number = "01",
        title = "ENCRYPT",
        subtitle = "Zero-Knowledge Ingestion",
        description = "Customer messages arriving via WhatsApp are instantly converted into encrypted tensors. Private customer telephone numbers, sensitive intent, and transaction details are shielded before hitting model parameters.",
        formulaOrSpec = "E_pk(m) -> Homomorphic Ciphertext / AES-GCM 256",
        status = "ACTIVE • 0.04ms"
    ),
    TechStep(
        number = "02",
        title = "COMPUTE",
        subtitle = "Autonomous Neural Reasoning",
        description = "The AI employee processes availability, reads catalog schemas, parses natural dates ('this Saturday at 2pm'), negotiates pricing tiers, and formulates human-level conversational responses without exposing unencrypted state.",
        formulaOrSpec = "Eval(f, c_1, ..., c_k) -> Ciphertext Result",
        status = "INFERENCE • 142 tok/s"
    ),
    TechStep(
        number = "03",
        title = "PROVE",
        subtitle = "Deterministic Guardrails",
        description = "Every generated booking, invoice, and catalog recommendation generates a verifiable cryptographic receipt ensuring zero hallucinations, strict inventory bounds, and business rules compliance.",
        formulaOrSpec = "zk-SNARK Pi : R(x, w) == 1",
        status = "VERIFIED • 100%"
    ),
    TechStep(
        number = "04",
        title = "VERIFY",
        subtitle = "Autonomous WhatsApp Action",
        description = "The verified resolution is committed directly to the business calendar, inventory database, and dispatched back to the customer's WhatsApp in natural, friendly prose within sub-second latency.",
        formulaOrSpec = "Commit(Calendar + WhatsApp Dispatch)",
        status = "SETTLED • 24/7/365"
    )
)

data class WhatsAppComparison(
    val dimension: String,
    val whatsAppBusiness: String,
    val zamaAiEmployee: String
)

val WHATSAPP_COMPARISONS = listOf(
    WhatsAppComparison(
        dimension = "Customer asks price",
        whatsAppBusiness = "Static catalog link or owner must reply",
        zamaAiEmployee = "AI understands question & quotes exact price instantly"
    ),
    WhatsAppComparison(
        dimension = "Product & service lookup",
        whatsAppBusiness = "Static PDF or fixed catalog images",
        zamaAiEmployee = "AI dynamically recommends best options based on customer needs"
    ),
    WhatsAppComparison(
        dimension = "Quick replies",
        whatsAppBusiness = "Generic template buttons ('Thanks for contacting!')",
        zamaAiEmployee = "AI synthesizes tailored, context-aware answers"
    ),
    WhatsAppComparison(
        dimension = "Customer asks for appointment",
        whatsAppBusiness = "Owner checks book manually & replies hours later",
        zamaAiEmployee = "AI checks real-time calendar availability in 1.2s"
    ),
    WhatsAppComparison(
        dimension = "Relative dates ('this Saturday 2pm')",
        whatsAppBusiness = "Can't understand relative language",
        zamaAiEmployee = "Understands relative dates, times, and timezones"
    ),
    WhatsAppComparison(
        dimension = "Customer doesn't respond",
        whatsAppBusiness = "Chats go cold and lost forever",
        zamaAiEmployee = "Polite, timely autonomous follow-ups that close sales"
    ),
    WhatsAppComparison(
        dimension = "Quotation requested",
        whatsAppBusiness = "Owner opens spreadsheet/laptop manually",
        zamaAiEmployee = "AI instantly compiles personalized quotation breakdown"
    ),
    WhatsAppComparison(
        dimension = "Booking confirmation",
        whatsAppBusiness = "Manual calendar entry & paper notebook",
        zamaAiEmployee = "AI reserves slot, locks calendar, sends confirmation"
    ),
    WhatsAppComparison(
        dimension = "Complex edge-case question",
        whatsAppBusiness = "Customer left on 'Seen'",
        zamaAiEmployee = "AI gracefully drafts summary & escalates to owner"
    ),
    WhatsAppComparison(
        dimension = "Analytics & Sales Pulse",
        whatsAppBusiness = "Basic message count & unread labels",
        zamaAiEmployee = "Full executive neural dashboard & revenue analytics"
    )
)

data class ChatMessage(
    val id: String,
    val isFromCustomer: Boolean,
    val text: String,
    val timestamp: String,
    val statusTicks: String = "✓✓",
    val isActionCard: Boolean = false,
    val actionDetail: String? = null,
    val isVoiceNote: Boolean = false,
    val audioModelUsed: String? = null
) {
    val isUser: Boolean get() = isFromCustomer
    val isAi: Boolean get() = !isFromCustomer
    val senderType: String get() = if (isFromCustomer) "USER" else "AI"
}

data class SalonService(
    val id: String,
    val name: String,
    val startingPrice: String,
    val duration: String,
    val category: String,
    val description: String
)

val SAMPLE_SALON_SERVICES = listOf(
    SalonService("1", "Knotless Braids", "R650", "2.5 hrs", "Hair Styling", "Clean parts, lightweight, natural scalp finish."),
    SalonService("2", "Goddess Box Braids", "R850", "3.0 hrs", "Hair Styling", "Boho curly tendrils with premium human hair accents."),
    SalonService("3", "Silk Press & Deep Treatment", "R500", "1.5 hrs", "Treatment", "Thermal smoothing with intense argan moisture therapy."),
    SalonService("4", "Bridal Luxury Updo & Makeup", "R1,400", "2.5 hrs", "Special Occasions", "Full bridal glamour trial and setting veil placement."),
    SalonService("5", "Lash Extensions (Volume)", "R450", "1.2 hrs", "Beauty", "Soft lightweight Russian volume set.")
)
