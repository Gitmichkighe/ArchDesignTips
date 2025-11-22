import json

# 1. Read input JSON
with open("Architips_v1.json", "r", encoding="utf-8") as f:
    categories = json.load(f)

# 2. Define desired category order
desired_order = [
    "Design Strategy and Human Experience",
    "Design Process",
    "Design Process and Coordination",
    "The Designer’s Eye: Aesthetic Insights",
    "Adjacency & Spatial Relationships",
    "Circulation and Layout",
    "Doors Windows and Openings",
    "Lighting and Ventilation",
    "Passive Design and Climate Response",
    "Universal and Accessible Design",
    "Kitchen Design",
    "Bathroom Design",
    "Bedroom Design",
    "Residential Design",
    "Multifamily and Commercial Design",
    "Commercial Design",
    "Educational and Institutional Design",
    "Industrial and Warehouse Design",
    "Storage and Organization",
    "Furniture Design and Ergonomics",
    "Building Fixtures and Fittings",
    "Materials and Finishes",
    "Material Selection and Sustainability",
    "Sustainability and Efficiency",
    "Biophilic Design",
    "Acoustics and Sound Control",
    "MEP and Utilities Integration",
    "Structural Systems and Framing",
    "Roofing Systems and Design",
    "Smart Home and IoT Integration",
    "Parking Design and Vehicular Access",
    "Urban Design and Planning",
    "Landscaping",
    "Zoning and Land Use Regulations",
    "Codes, Safety and Accessibility",
    "Architecture codes by country",
    "Emergency Planning & Disaster-Resilient Design",
    "Site Safety and Risk Management",
    "Renovation and Retrofit Principles",
    "Historical Preservation and Adaptive Reuse",
    "Flexible Design to Facilitate Future Changes",
    "Digital Fabrication and Parametric Design",
    "Presentation, Visualization, and Graphics",
    "Global Architecture Trends & Case Studies",
    "Cultural, Social & Inclusive Design",
    "Behavioral & Post-Occupancy Studies",
    "Construction and Maintenance",
    "Construction Contracts and Project Delivery Methods",
    "Cost Estimation and Budgeting",
    "Lifecycle Assessment and Post-Occupancy Evaluation",
    "Project Management & Team Collaboration",
    "Legal, Insurance & Risk Considerations",
    "Tiny Homes and Compact Living"
]

# 3. Map categories by name for fast lookup
category_map = {item["category"]: item for item in categories}

# 4. Build sorted list
sorted_categories = []

# Add categories in desired order
for cat_name in desired_order:
    if cat_name in category_map:
        sorted_categories.append(category_map[cat_name])
        del category_map[cat_name]

# Append any remaining categories not in desired order
sorted_categories.extend(category_map.values())

# 5. Write sorted JSON to output file
with open("output_sorted.json", "w", encoding="utf-8") as f:
    json.dump(sorted_categories, f, indent=4, ensure_ascii=False)

print("JSON categories sorted successfully! Output saved to 'output_sorted.json'.")
