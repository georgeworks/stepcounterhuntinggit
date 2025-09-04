package com.example.stepcounterhunting

import android.content.Context
import android.content.SharedPreferences

object DataManager {
    private lateinit var prefs: SharedPreferences
    private lateinit var stepPrefs: SharedPreferences
    private val collection = mutableListOf<Animal>()
    private val exploredRegions = mutableSetOf<String>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("StepCounterData", Context.MODE_PRIVATE)
        stepPrefs = context.getSharedPreferences("StepCounter", Context.MODE_PRIVATE)

        // Check if this is first app launch
        val isFirstLaunch = prefs.getBoolean("data_manager_initialized", false)
        if (!isFirstLaunch) {
            // First time initialization - clear everything
            prefs.edit().clear().apply()
            stepPrefs.edit().clear().apply()

            // Mark as initialized
            prefs.edit().putBoolean("data_manager_initialized", true).apply()

            // Initialize with clean state
            collection.clear()
            exploredRegions.clear()
        } else {
            // Normal initialization - load existing data
            loadData()
        }
    }

    private fun loadData() {
        val collectionString = prefs.getString("collection", "") ?: ""
        if (collectionString.isNotEmpty()) {
            val animalIds = collectionString.split(",")
            collection.clear()
            animalIds.forEach { id ->
                findAnimalById(id)?.let { collection.add(it) }
            }
        }

        val regionsString = prefs.getString("explored_regions", "") ?: ""
        if (regionsString.isNotEmpty()) {
            exploredRegions.clear()
            exploredRegions.addAll(regionsString.split(","))
        }
    }

    private fun saveData() {
        val collectionString = collection.joinToString(",") { it.id }
        prefs.edit().putString("collection", collectionString).apply()

        val regionsString = exploredRegions.joinToString(",")
        prefs.edit().putString("explored_regions", regionsString).apply()
    }

    private fun findAnimalById(id: String): Animal? {
        val allAnimals = usRegions.flatMap { it.animals } + getDefaultAnimals()
        return allAnimals.find { it.id == id }
    }

    fun addToCollection(animal: Animal): Boolean {
        // Check if it's a duplicate
        val isDuplicate = collection.any { it.id == animal.id }

        collection.add(animal)
        saveData()

        // If it's a duplicate, award a lure
        if (isDuplicate) {
            val currentLures = getLureCount()
            setLureCount(currentLures + 1)
        }

        return isDuplicate
    }

    fun getLureCount(): Int {
        return prefs.getInt("lure_count", 0)
    }

    fun setLureCount(count: Int) {
        prefs.edit().putInt("lure_count", count).apply()
    }

    fun useLure() {
        val currentLures = getLureCount()
        if (currentLures > 0) {
            setLureCount(currentLures - 1)
        }
    }

    fun getCollection(): List<Animal> = collection.toList()

    fun addExploredRegion(region: String) {
        exploredRegions.add(region)
        saveData()
    }

    fun getExploredRegions(): Set<String> = exploredRegions.toSet()

    fun getStats(): UserStats {
        return UserStats(
            totalSteps = stepPrefs.getInt("total_lifetime_steps", 0),
            animalsCollected = collection.size,
            regionsExplored = exploredRegions.size
        )
    }

    fun getDefaultAnimals(): List<Animal> {
        return listOf(
            Animal(
                "default_1",
                "Common Animal",
                "Basic creature",
                Rarity.COMMON,
                "Default",
                "This mysterious creature adapts to any environment!"
            ),
            Animal(
                "default_2",
                "Uncommon Animal",
                "Interesting creature",
                Rarity.UNCOMMON,
                "Default",
                "Scientists are still discovering new facts about this species!"
            ),
            Animal(
                "default_3",
                "Rare Animal",
                "Hard to find",
                Rarity.RARE,
                "Default",
                "Only a few hundred of these animals have ever been documented!"
            ),
            Animal(
                "default_4",
                "Epic Animal",
                "Very special",
                Rarity.EPIC,
                "Default",
                "This creature has abilities that seem almost supernatural!"
            ),
            Animal(
                "default_5",
                "Legendary Animal",
                "Extremely rare",
                Rarity.LEGENDARY,
                "Default",
                "Some say this animal doesn't exist, but you've proven them wrong!"
            )
        )
    }

    val usRegions = listOf(
        Region(
            "The Appalachians",
            listOf(
                Animal(
                    "app_1",
                    "Hellbender",
                    "Giant Appalachian salamander",
                    Rarity.RARE,
                    "The Appalachians",
                    "North America’s largest fully aquatic salamander haunts cold, rocky streams from New York to Georgia. It breathes through skin folds, clinging under boulders in swift currents. Conservationists call it a “river monster” in need of clean, oxygen-rich water."
                ),
                Animal(
                    "app_2",
                    "Appalachian Cottontail",
                    "High-elevation native rabbit",
                    Rarity.UNCOMMON,
                    "The Appalachians",
                    "Found mainly in the central and southern Appalachians, this shy rabbit sticks to dense mountain thickets. It’s often confused with the common eastern cottontail, but DNA and subtle field marks tell a different story. Seeing one usually means you’ve climbed into true high-country habitat."
                ),
                Animal(
                    "app_3",
                    "Timber Rattlesnake",
                    "Iconic forest viper",
                    Rarity.UNCOMMON,
                    "The Appalachians",
                    "With cryptic bands and a velvet-black tail, timber rattlesnakes coil on sun-warmed rock ledges throughout Appalachian ridges. They overwinter in communal dens and can live for decades. Despite their reputation, they’re remarkably calm unless provoked."
                ),
                Animal(
                    "app_4",
                    "Brook Trout",
                    "Coldwater native char",
                    Rarity.COMMON,
                    "The Appalachians",
                    "The only trout native to much of the East, “brookies” glow with blue halos and pumpkin fins in icy headwaters. Their presence is a living water-quality test—warm, silted streams push them out. Anglers revere them as jewels of Appalachian creeks."
                ),
                Animal(
                    "app_5",
                    "Cerulean Warbler",
                    "Sky-blue canopy singer",
                    Rarity.RARE,
                    "The Appalachians",
                    "This tiny migrant nests high in mature Appalachian forests where it sings a buzzy trill from the treetops. Populations have fallen with canopy fragmentation, making prime ridge-top habitat a precious resource. Spotting one feels like catching a scrap of sky in motion."
                ),
                Animal(
                    "app_6",
                    "Allegheny Woodrat",
                    "Rockhouse packrat",
                    Rarity.RARE,
                    "The Appalachians",
                    "A cliff-and-cave specialist of the Appalachian Plateau, this native woodrat gathers shiny treasures to decorate stick nests. Parasites and habitat changes have shrunk its range. Finding its midden piles is like stumbling onto a tiny mountain museum."
                ),
                Animal(
                    "app_7",
                    "Spruce-fir Moss Spider",
                    "Miniature high-peak hunter",
                    Rarity.LEGENDARY,
                    "The Appalachians",
                    "Smaller than your pinky’s fingernail, this endangered tarantula-relative lives only on cool, mossy rock outcrops atop southern Appalachian summits. It depends on shade from red spruce and Fraser fir forests. Climate warming and tree decline put it on a razor’s edge."
                ),
                Animal(
                    "app_8",
                    "Ruffed Grouse",
                    "Drumming forest bird",
                    Rarity.UNCOMMON,
                    "The Appalachians",
                    "Males beat their wings on logs to make a deep ‘drum’ that pulses through Appalachian hollows. The species thrives in young, regenerating forest, waxing and waning in cycles. In winter it ‘snow-roosts,’ diving into powder to sleep insulated from the cold."
                ),
                Animal(
                    "app_9",
                    "Cheat Mountain Salamander",
                    "West Virginia highland endemic",
                    Rarity.RARE,
                    "The Appalachians",
                    "Restricted to a handful of spruce-topped ridges in West Virginia, this dusky salamander never strays far from moist moss and leaf litter. It lacks lungs and absorbs oxygen through its skin. Entire global fortunes of the species fit within a few mountaintops."
                ),
                Animal(
                    "app_10",
                    "Virginia Big-eared Bat",
                    "Cave bat with supersized ears",
                    Rarity.EPIC,
                    "The Appalachians",
                    "This federally endangered bat roosts in Appalachian caves and cliffline crevices. Its huge ears swivel like satellite dishes to catch faint insect buzzes. Quiet caves, dark nights, and clean summer foraging grounds keep colonies alive."
                )
            )
        ),
        Region(
            "Desert Southwest",
            listOf(
                Animal(
                    "sw_1",
                    "Greater Roadrunner",
                    "Desert sprinter",
                    Rarity.UNCOMMON,
                    "Desert Southwest",
                    "Built for heat and speed, roadrunners dash at highway-pace across Sonoran and Chihuahuan scrub. They pounce on lizards and even juvenile rattlesnakes with lightning bills. Sunbathing with a flared back warms them on cool mornings."
                ),
                Animal(
                    "sw_2",
                    "Gila Monster",
                    "Venomous beaded lizard",
                    Rarity.RARE,
                    "Desert Southwest",
                    "One of the world’s only venomous lizards, the Gila monster stores fat in its tail and spends much of life underground. Beadlike scales shine like desert mosaics. Its slow gait hides a powerful bite—look, don’t touch."
                ),
                Animal(
                    "sw_3",
                    "Desert Bighorn Sheep",
                    "Cliff-climbing ungulate",
                    Rarity.RARE,
                    "Desert Southwest",
                    "Rams crash horns at 40 mph on red-rock amphitheaters and walk away unshattered. Rubberized hooves grip slick sandstone where few predators can follow. Ewes lead nimble lambs along knife-edge ledges by their first spring."
                ),
                Animal(
                    "sw_4",
                    "Sonoran Pronghorn",
                    "Heat-hardened speedster",
                    Rarity.EPIC,
                    "Desert Southwest",
                    "A desert-adapted cousin of the plains form, Sonoran pronghorn endure blistering summers with sparse water. Their 60 mph bursts are second only to cheetahs. Conservation herds on U.S.–Mexico borderlands are racing time."
                ),
                Animal(
                    "sw_5",
                    "Desert Tortoise",
                    "Slow-and-steady survivor",
                    Rarity.UNCOMMON,
                    "Desert Southwest",
                    "Spending most of life in burrows, this tortoise waits out scorching heat and flash floods. It stocks up on spring greens, then sips water stored in its bladder for months. A single heavy rain can refill the tank."
                ),
                Animal(
                    "sw_6",
                    "Harris’s Hawk",
                    "Pack-hunting raptor",
                    Rarity.UNCOMMON,
                    "Desert Southwest",
                    "Unlike most raptors, Harris’s hawks hunt cooperatively, leapfrogging from cactus to cactus to flush prey. They perch on saguaro “watchtowers” and share meals among the team. In the U.S., the species is most at home in the Southwest."
                ),
                Animal(
                    "sw_7",
                    "Collared Peccary (Javelina)",
                    "Cactus-foraging native",
                    Rarity.COMMON,
                    "Desert Southwest",
                    "Small, tough, and social, javelinas trot in bands that nose prickly pear pads like green pancakes. Musk glands mark trails and beds. Their chattering teeth and stiff mohawks make any desert stroll unforgettable."
                ),
                Animal(
                    "sw_8",
                    "Kangaroo Rat",
                    "No-water desert rodent",
                    Rarity.COMMON,
                    "Desert Southwest",
                    "These night jumpers never need to drink—kidneys and metabolism pull water from seeds. Cheek pouches harvest grain like living saddlebags. One kick sends a cloud of sand and a leaping escape."
                ),
                Animal(
                    "sw_9",
                    "Elf Owl",
                    "Tiny saguaro tenant",
                    Rarity.RARE,
                    "Desert Southwest",
                    "The world’s smallest owl nests in old woodpecker holes in giant cacti and mesquite snags. Cricket-sized prey fuels its frenetic nights. A faint squeak betrays a predator no taller than a smartphone."
                ),
                Animal(
                    "sw_10",
                    "Western Diamondback Rattlesnake",
                    "Desert buzztail",
                    Rarity.UNCOMMON,
                    "Desert Southwest",
                    "Bold chevrons and a black-and-white tail tip warn hikers across the Southwest. Diamondbacks regulate encounters with that famous rattle—most strikes happen when they’re stepped on. Give them space and the trail stays friendly."
                )
            )
        ),
        Region(
            "Pacific Northwest",
            listOf(
                Animal(
                    "pnw_1",
                    "Northern Spotted Owl",
                    "Old-growth forest specialist",
                    Rarity.RARE,
                    "Pacific Northwest",
                    "Silent wings thread through cathedral-tall Douglas-fir and cedar. This owl needs big, old forests with broken canopies and deep shade. Habitat loss and barred owl competition turned it into an icon of Northwest conservation."
                ),
                Animal(
                    "pnw_2",
                    "Sea Otter",
                    "Kelp-forest guardian",
                    Rarity.EPIC,
                    "Pacific Northwest",
                    "Floating on its back, the sea otter uses stones as anvils to crack urchins and clams. By thinning urchins, it protects kelp forests that shelter fish and blunt waves. A handful of reintroduced populations now anchor coastal recovery."
                ),
                Animal(
                    "pnw_3",
                    "Roosevelt Elk",
                    "Massive coastal elk",
                    Rarity.UNCOMMON,
                    "Pacific Northwest",
                    "The largest elk subspecies in North America browses rain-drenched river bottoms and ferny glades. Bulls bugle through fog that beads on mossy limbs. Herds step from mist like moving tree trunks."
                ),
                Animal(
                    "pnw_4",
                    "Marbled Murrelet",
                    "Secretive seabird of giants",
                    Rarity.RARE,
                    "Pacific Northwest",
                    "By day it fishes far offshore; by night it flies inland to nest on wide, mossy limbs of ancient conifers. The nest is often just a moss pad 150 feet up. Discovery of its nesting habit stunned biologists in the 1970s."
                ),
                Animal(
                    "pnw_5",
                    "Pacific Giant Salamander",
                    "Barking stream salamander",
                    Rarity.UNCOMMON,
                    "Pacific Northwest",
                    "This hefty amphibian prowls cold forest creeks and can emit a tiny bark when disturbed. Larvae linger for years with feathery gills under submerged stones. Old logs and clean water are non-negotiable."
                ),
                Animal(
                    "pnw_6",
                    "Banana Slug",
                    "Rainforest decomposer",
                    Rarity.COMMON,
                    "Pacific Northwest",
                    "As long as a dinner roll and often bright yellow, banana slugs keep the forest recycling. Their slime can numb a predator’s tongue and even act like a glue. After rain, trails turn into slow-motion highways."
                ),
                Animal(
                    "pnw_7",
                    "Pacific Fisher",
                    "Shadow of the understory",
                    Rarity.RARE,
                    "Pacific Northwest",
                    "A lithe, cat-sized mustelid that threads through blowdowns and vine maple like smoke. One of the few predators that regularly tackles porcupines. Reintroductions are stitching populations back into the coastal ranges."
                ),
                Animal(
                    "pnw_8",
                    "Tufted Puffin",
                    "Wild-haired seabird",
                    Rarity.RARE,
                    "Pacific Northwest",
                    "Summer brings golden head plumes and fire-orange bills to offshore colonies. Puffins fly underwater with whirring wings and carry fish crosswise like silver mustaches. Their cliff burrows peer out over crashing surf."
                ),
                Animal(
                    "pnw_9",
                    "Dungeness Crab",
                    "Cold-water crustacean",
                    Rarity.UNCOMMON,
                    "Pacific Northwest",
                    "A culinary star of bays and eelgrass flats, Dungeness crabs scuttle on armored tiptoes. Males migrate to find mates as winter storms pound the coast. Their molted shells litter beaches like purple-rimmed pottery."
                ),
                Animal(
                    "pnw_10",
                    "Gray Whale",
                    "Coastal migrant",
                    Rarity.EPIC,
                    "Pacific Northwest",
                    "Each spring, gray whales hug the coastline on a 10,000-mile journey between Mexico and the Arctic. Some linger to “bubble blast” shrimp from muddy shallows. From headlands, you can watch spouts stitch the horizon."
                )
            )
        ),
        Region(
            "Great Plains",
            listOf(
                Animal(
                    "gp_1",
                    "American Bison",
                    "Thundering prairie giant",
                    Rarity.EPIC,
                    "Great Plains",
                    "Once numbering in the millions, bison shaped grasslands from Texas to the Dakotas. Their hooves churned soil and seeded wildflowers. Today, restored herds anchor a rebounding prairie story."
                ),
                Animal(
                    "gp_2",
                    "Greater Prairie-Chicken",
                    "Booming grassland dancer",
                    Rarity.RARE,
                    "Great Plains",
                    "At dawn, males inflate orange neck sacs and stamp their feet in a rattling blur. These ‘leks’ are grassland stages passed down for generations. Without tallgrass, the dance falls silent."
                ),
                Animal(
                    "gp_3",
                    "Black-tailed Prairie Dog",
                    "Town-building rodent",
                    Rarity.COMMON,
                    "Great Plains",
                    "Prairie dogs engineer sprawling ‘towns’ with air-conditioned tunnels and lookout mounds. Their chirps form a complex alarm language that can describe predators. Whole ecosystems revolve around their digging."
                ),
                Animal(
                    "gp_4",
                    "Burrowing Owl",
                    "Ground-nesting owl",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "Long legs and bright eyes give this owl a perpetual look of surprise. It nests in abandoned prairie dog tunnels and bobs its head at intruders. Dusk patrols skim like brown bumblebees over grass."
                ),
                Animal(
                    "gp_5",
                    "Ferruginous Hawk",
                    "Largest North American hawk",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "This pale, big-winged hawk hovers over open country hunting ground squirrels and jackrabbits. On cold days it wears a rusty ‘vest’ of feathers. Fencepost silhouettes are often your first clue."
                ),
                Animal(
                    "gp_6",
                    "Swift Fox",
                    "Coyote-dodging sprinter",
                    Rarity.RARE,
                    "Great Plains",
                    "Barely cat-sized, swift foxes streak between sage and bluestem at freeway speeds for their size. They den on slight rises to scan for danger. Reintroductions are repopulating old haunts."
                ),
                Animal(
                    "gp_7",
                    "Ornate Box Turtle",
                    "Painted prairie reptile",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "High-domed shells wear yellow sunbursts on dark backgrounds, like portable prairies. These turtles bury themselves to escape heat and drought. Road crossings are their toughest modern gauntlet."
                ),
                Animal(
                    "gp_8",
                    "American Badger",
                    "Ferocious digger",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "Shovel-like claws and a low gear make badgers excavation pros. They flip sod for ground squirrels and share hunts with coyotes in odd partnerships. Fresh dirt fans mark their work sites."
                ),
                Animal(
                    "gp_9",
                    "Whooping Crane",
                    "Towering white migrant",
                    Rarity.LEGENDARY,
                    "Great Plains",
                    "Among the world’s rarest birds, whoopers stop in Nebraska’s Platte River on a continent-spanning commute. At five feet tall, they stand eye-to-eye with deer. Careful protections shepherd every family south and back again."
                ),
                Animal(
                    "gp_10",
                    "Bison Beetle",
                    "Prairie cleanup crew",
                    Rarity.RARE,
                    "Great Plains",
                    "This dung beetle specializes in bison patties, rolling and burying nutrient-rich balls that feed the soil. It’s a tiny engineer behind prairie fertility. Where bison return, the beetles’ industry follows."
                )
            )
        ),
        Region(
            "Far North",
            listOf(
                Animal(
                    "north_1",
                    "Moose",
                    "Giant of boreal forests",
                    Rarity.EPIC,
                    "Far North",
                    "Standing seven feet at the shoulder, moose browse willow thickets and wade into icy lakes for aquatic plants. They can swim for miles and dive to the bottom for greens. Antlers on big bulls can span six feet like living fences."
                ),
                Animal(
                    "north_2",
                    "Muskox",
                    "Shaggy Ice Age relic",
                    Rarity.RARE,
                    "Far North",
                    "Muskoxen face blizzards in tight defensive circles, calves at the center like a living fortress. Their qiviut underwool is softer and warmer than cashmere. Alaska’s tundra still echoes with their prehistoric presence."
                ),
                Animal(
                    "north_3",
                    "Caribou",
                    "Wide-roaming reindeer",
                    Rarity.UNCOMMON,
                    "Far North",
                    "These long-distance migrants track green-up across alpine and Arctic. Both males and females grow antlers, rare among deer. Hooves splay into snowshoes and paddle across icy rivers."
                ),
                Animal(
                    "north_4",
                    "Brown Bear (Coastal Grizzly)",
                    "Salmon-powered omnivore",
                    Rarity.EPIC,
                    "Far North",
                    "From Katmai to Kodiak, coastal brown bears grow enormous on salmon runs and sedge meadows. Cubs learn fishing from mom like a family trade. A single bear can snag twenty fish in a tide cycle."
                ),
                Animal(
                    "north_5",
                    "Polar Bear",
                    "Sea-ice specialist",
                    Rarity.LEGENDARY,
                    "Far North",
                    "The planet’s top Arctic predator patrols pack ice along Alaska’s North Slope. It can smell a seal’s breathing hole from miles away. Transparent fur and a black skin coat turn sunlight into stealthy warmth."
                ),
                Animal(
                    "north_6",
                    "Arctic Fox",
                    "Snow-white nomad",
                    Rarity.UNCOMMON,
                    "Far North",
                    "Winter coats turn ghost-white and summer coats brown to match tundra seasons. When lemmings boom, fox families balloon too. They trail polar bears to scavenge leftovers on the sea ice."
                ),
                Animal(
                    "north_7",
                    "Snowy Owl",
                    "Silent tundra sentinel",
                    Rarity.RARE,
                    "Far North",
                    "Bright white owls scan for voles from drifted hummocks. Some winters they irrupt south in dazzling numbers. Feathered feet act like down booties against razor wind."
                ),
                Animal(
                    "north_8",
                    "Steller Sea Lion",
                    "North Pacific heavyweight",
                    Rarity.UNCOMMON,
                    "Far North",
                    "Bellowing males guard crowded rookeries on remote islands. Pups learn to porpoise through kelp forests as salmon schools flash by. Alaska’s outer coast is their noisy summer stage."
                ),
                Animal(
                    "north_9",
                    "Pacific Walrus",
                    "Ivory-tusked bottom-feeder",
                    Rarity.RARE,
                    "Far North",
                    "Walruses haul out by the thousands on remote beaches when sea ice retreats. Whiskers probe the seafloor for clams they vacuum like living shop-vacs. Tusks help them lever huge bodies back onto ice."
                ),
                Animal(
                    "north_10",
                    "King Eider",
                    "Arctic jewel duck",
                    Rarity.UNCOMMON,
                    "Far North",
                    "Males wear kaleidoscope bills and mint-green necks that look painted on. They raft by the hundreds off Alaska’s coasts in spring migration. Diving deep, they pluck mussels from frigid currents without a shiver."
                )
            )
        )
    )
}