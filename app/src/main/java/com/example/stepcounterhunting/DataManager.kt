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
                Rarity.RARE,
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
                // COMMON (3)
                Animal(
                    "app_1",
                    "Brook Trout",
                    "Coldwater native char",
                    Rarity.COMMON,
                    "The Appalachians",
                    "The only native trout across much of Appalachia, glowing with blue halos and pumpkin fins. It needs icy, ultra-clean streams—if brookies thrive, your water’s healthy. Anglers call them the jewels of mountain creeks."
                ),
                Animal(
                    "app_2",
                    "American Black Bear",
                    "Forest omnivore",
                    Rarity.COMMON,
                    "The Appalachians",
                    "Expert climbers that can sprint 30 mph, black bears roam oak ridges and berry thickets. Their noses are so keen they can smell food miles away. Most encounters end with a huff and a retreat."
                ),
                Animal(
                    "app_3",
                    "Ruffed Grouse",
                    "Drumming forest bird",
                    Rarity.COMMON,
                    "The Appalachians",
                    "Males “drum” by beating their wings on logs, a heartbeat thudding through spring hollows. Populations cycle with young forest growth. In deep winter they plunge into powder to sleep warmly under the snow."
                ),
                // UNCOMMON (2)
                Animal(
                    "app_4",
                    "Timber Rattlesnake",
                    "Iconic forest viper",
                    Rarity.COMMON,
                    "The Appalachians",
                    "Velvet-black tails and banded coils blend into leaf litter on sun-warmed outcrops. They overwinter in communal dens for decades. Calm by nature, most rattles are warnings—step back and everyone’s happy."
                ),
                Animal(
                    "app_5",
                    "Appalachian Cottontail",
                    "High-elevation native rabbit",
                    Rarity.UNCOMMON,
                    "The Appalachians",
                    "A secretive rabbit of cool, brushy mountaintops, often misidentified as its common cousin. DNA and subtle field marks tell it apart. Finding one means you’ve climbed into true high-country habitat."
                ),
                // RARE (2)
                Animal(
                    "app_6",
                    "Hellbender",
                    "Giant Appalachian salamander",
                    Rarity.UNCOMMON,
                    "The Appalachians",
                    "North America’s largest fully aquatic salamander hides under boulders in cold, fast rivers. It “breathes” through skin folds that need oxygen-rich water. Clean streams mean healthy hellbenders."
                ),
                Animal(
                    "app_7",
                    "Cerulean Warbler",
                    "Sky-blue canopy singer",
                    Rarity.UNCOMMON,
                    "The Appalachians",
                    "This tiny migrant nests high in mature forest canopies, a buzzy trill floating from ridge to ridge. Habitat loss hit it hard, making intact Appalachians vital. Spotting one feels like catching a scrap of sky."
                ),
                // EPIC (2)
                Animal(
                    "app_8",
                    "Virginia Big-eared Bat",
                    "Cave bat with huge ears",
                    Rarity.RARE,
                    "The Appalachians",
                    "Endangered bats that roost in cool caves and cliffline crevices. Their oversized ears swivel to home in on faint insect buzzes. Quiet caves and dark summer foraging routes keep colonies alive."
                ),
                Animal(
                    "app_9",
                    "Cheat Mountain Salamander",
                    "WV highland endemic",
                    Rarity.RARE,
                    "The Appalachians",
                    "Restricted to a handful of spruce-capped ridges in West Virginia. Lungless and moisture-loving, it absorbs oxygen through its skin. The species’ entire world fits within a few mountaintops."
                ),
                // LEGENDARY (1)
                Animal(
                    "app_10",
                    "Spruce-fir Moss Spider",
                    "Miniature peak-dweller",
                    Rarity.LEGENDARY,
                    "The Appalachians",
                    "Smaller than a fingernail and clinging to moss mats on the highest summits. Dependent on cool, shaded rock amid red spruce and Fraser fir. Warming peaks put this Appalachian original on a razor’s edge."
                )
            )
        ),
        Region(
            "Desert Southwest",
            listOf(
                // COMMON (3)
                Animal(
                    "sw_1",
                    "Greater Roadrunner",
                    "Desert sprinter",
                    Rarity.COMMON,
                    "Desert Southwest",
                    "Built for heat and speed, roadrunners dash at 20 mph across Sonoran scrub. They out-feint lizards and even small rattlesnakes. Morning sunbathing on dark skin patches tops up their warmth."
                ),
                Animal(
                    "sw_2",
                    "Collared Peccary (Javelina)",
                    "Cactus forager",
                    Rarity.COMMON,
                    "Desert Southwest",
                    "Stocky and social, javelinas trot in bands that nose prickly-pear pads like green pancakes. Musk glands mark trails and beds. Chattering teeth and stiff mohawks make any desert stroll memorable."
                ),
                Animal(
                    "sw_3",
                    "Kangaroo Rat",
                    "No-water seed hoarder",
                    Rarity.COMMON,
                    "Desert Southwest",
                    "Masters of dry living, they never drink—kidneys and metabolism pull water from seeds. Cheek pouches carry harvests like saddlebags. One kick and they vanish in a cloud of sand."
                ),
                // UNCOMMON (2)
                Animal(
                    "sw_4",
                    "Desert Tortoise",
                    "Slow-and-steady burrower",
                    Rarity.COMMON,
                    "Desert Southwest",
                    "Spending most of life underground, it rides out scorching summers and sudden floods. Spring greens fuel months of stored water. After a single good rain, it tanks up like a living canteen."
                ),
                Animal(
                    "sw_5",
                    "Harris’s Hawk",
                    "Pack-hunting raptor",
                    Rarity.UNCOMMON,
                    "Desert Southwest",
                    "Unlike most hawks, they hunt cooperatively—leapfrogging from saguaro to saguaro. Family groups share lookout ‘towers’ and meals. The Southwest is their U.S. stronghold."
                ),
                // RARE (2)
                Animal(
                    "sw_6",
                    "Gila Monster",
                    "Venomous beaded lizard",
                    Rarity.UNCOMMON,
                    "Desert Southwest",
                    "A handful of truly venomous lizards on Earth, and this is one. Beadlike scales glow like desert mosaics. Slow and sturdy, but its bite means business—look, don’t touch."
                ),
                Animal(
                    "sw_7",
                    "Elf Owl",
                    "Tiny saguaro tenant",
                    Rarity.UNCOMMON,
                    "Desert Southwest",
                    "The world’s smallest owl nests in abandoned woodpecker holes in cacti and mesquite. A faint squeak betrays a hunter barely taller than a smartphone. Crickets and moths power its midnight patrols."
                ),
                // EPIC (2)
                Animal(
                    "sw_8",
                    "Desert Bighorn Sheep",
                    "Cliff-climbing ungulate",
                    Rarity.RARE,
                    "Desert Southwest",
                    "Rams clash at 40 mph on red-rock amphitheaters and walk away unshattered. Rubberized hooves grip slick sandstone where few predators can follow. Ewes teach lambs knife-edge ledges by spring."
                ),
                Animal(
                    "sw_9",
                    "Sonoran Pronghorn",
                    "Heat-hardened speedster",
                    Rarity.RARE,
                    "Desert Southwest",
                    "A desert-adapted cousin of the plains form, built for blistering summers with sparse water. Bursts hit 60 mph—second only to cheetahs. Recovery herds straddle the U.S.–Mexico borderlands."
                ),
                // LEGENDARY (1)
                Animal(
                    "sw_10",
                    "California Condor",
                    "Ten-foot-wing soaring scavenger",
                    Rarity.LEGENDARY,
                    "Desert Southwest",
                    "North America’s largest bird rides hot thermals over canyon country of AZ/UT/CA. Lead-free landscapes and careful releases brought them back. A shadow passing overhead feels prehistoric."
                )
            )
        ),
        Region(
            "Pacific Northwest",
            listOf(
                // COMMON (3)
                Animal(
                    "pnw_1",
                    "Banana Slug",
                    "Rainforest decomposer",
                    Rarity.COMMON,
                    "Pacific Northwest",
                    "Bright yellow and as long as a dinner roll, banana slugs keep the forest recycling. Their slime can numb predators’ tongues and even act like glue. After rain, trails become slow-motion highways."
                ),
                Animal(
                    "pnw_2",
                    "Dungeness Crab",
                    "Cold-water crustacean",
                    Rarity.COMMON,
                    "Pacific Northwest",
                    "A culinary star of bays and eelgrass flats, scuttling on armored tiptoes. Winter gales coincide with mating migrations. Molted shells rim beaches like purple-edged pottery."
                ),
                Animal(
                    "pnw_3",
                    "Black-tailed Deer",
                    "Coastal forest browser",
                    Rarity.COMMON,
                    "Pacific Northwest",
                    "These shade-loving deer thrive from foggy coast to foothill clearings. Their black-tipped tails flick like punctuation in the understory. Watch at dusk where salal meets meadow."
                ),
                // UNCOMMON (2)
                Animal(
                    "pnw_4",
                    "Roosevelt Elk",
                    "Massive coastal elk",
                    Rarity.COMMON,
                    "Pacific Northwest",
                    "The largest elk subspecies browses river bottoms and ferny glades. Bulls bugle through mist that beads on mossy limbs. Herds step from fog like moving tree trunks."
                ),
                Animal(
                    "pnw_5",
                    "Pacific Giant Salamander",
                    "Barking stream salamander",
                    Rarity.UNCOMMON,
                    "Pacific Northwest",
                    "Hefty amphibians prowl cold creeks and can emit a tiny bark when disturbed. Larvae linger for years with feathery gills under stones. Old logs and clean water are non-negotiable."
                ),
                // RARE (2)
                Animal(
                    "pnw_6",
                    "Northern Spotted Owl",
                    "Old-growth specialist",
                    Rarity.UNCOMMON,
                    "Pacific Northwest",
                    "Silent wings thread through cathedral-tall Douglas-fir and cedar. It needs big, broken-canopy forests and deep shade. Barred owl competition and logging made it an icon of conservation."
                ),
                Animal(
                    "pnw_7",
                    "Marbled Murrelet",
                    "Seabird of giant trees",
                    Rarity.UNCOMMON,
                    "Pacific Northwest",
                    "By day it fishes far offshore; by night it flies inland to nest on wide, mossy limbs of ancient conifers. The ‘nest’ is often just a moss pad 150 feet up. Its secret life stunned biologists when discovered."
                ),
                // EPIC (2)
                Animal(
                    "pnw_8",
                    "Sea Otter",
                    "Kelp-forest guardian",
                    Rarity.RARE,
                    "Pacific Northwest",
                    "On its back with a stone anvil, it cracks urchins and clams like a pocket blacksmith. By trimming urchins, it protects kelp forests that blunt waves and shelter fish. Reintroductions are stitching the coast back together."
                ),
                Animal(
                    "pnw_9",
                    "Gray Whale",
                    "Coastal migrant",
                    Rarity.RARE,
                    "Pacific Northwest",
                    "Each spring, whales trace a 10,000-mile commute between Mexico and the Arctic, spouting just offshore. Some linger to ‘bubble blast’ shrimp from muddy shallows. Headlands become front-row seats."
                ),
                // LEGENDARY (1)
                Animal(
                    "pnw_10",
                    "Killer Whale (Orca, SRKW)",
                    "Iconic fish-eating dolphin",
                    Rarity.LEGENDARY,
                    "Pacific Northwest",
                    "The Southern Resident orcas are tightly bonded family groups that specialize in salmon. Complex calls and matriarchal wisdom guide hunts through inland seas. Few marine animals feel more like neighbors."
                )
            )
        ),
        Region(
            "Great Plains",
            listOf(
                // COMMON (3)
                Animal(
                    "gp_1",
                    "Black-tailed Prairie Dog",
                    "Town-building rodent",
                    Rarity.COMMON,
                    "Great Plains",
                    "Prairie dogs engineer sprawling towns with air-conditioned tunnels and lookout mounds. Their chirps form a complex alarm language that can describe predators. Whole ecosystems revolve around their digging."
                ),
                Animal(
                    "gp_2",
                    "Western Meadowlark",
                    "Grassland songster",
                    Rarity.COMMON,
                    "Great Plains",
                    "A liquid, flute-like song pours from fenceposts after spring storms. Bright breast chevrons flash when it hops through bluestem. State bird in multiple Plains states for good reason."
                ),
                Animal(
                    "gp_3",
                    "Ornate Box Turtle",
                    "Painted prairie reptile",
                    Rarity.COMMON,
                    "Great Plains",
                    "High-domed shells wear yellow sunbursts on dark backgrounds like portable prairies. They burrow to escape heat and drought. Road crossings remain their toughest modern gauntlet."
                ),
                // UNCOMMON (2)
                Animal(
                    "gp_4",
                    "Burrowing Owl",
                    "Ground-nesting owl",
                    Rarity.COMMON,
                    "Great Plains",
                    "Long legs and bright eyes give a look of perpetual surprise. It nests in abandoned prairie dog tunnels and bobs its head at intruders. Dusk patrols skim like brown bumblebees over grass."
                ),
                Animal(
                    "gp_5",
                    "American Badger",
                    "Ferocious digger",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "Shovel-like claws and a low gear make badgers excavation pros. They flip sod for ground squirrels and sometimes even team up with coyotes. Fresh fans of dirt mark their work sites."
                ),
                // RARE (2)
                Animal(
                    "gp_6",
                    "Swift Fox",
                    "Coyote-dodging sprinter",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "Barely cat-sized, swift foxes streak between sage and bluestem at freeway speeds for their size. They den on slight rises to scan for danger. Reintroductions are repopulating old haunts."
                ),
                Animal(
                    "gp_7",
                    "Greater Prairie-Chicken",
                    "Booming dancer",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "At dawn, males inflate orange neck sacs and stamp their feet in a rattling blur. Leks are grassland stages passed down for generations. Without tallgrass, the dance falls silent."
                ),
                // EPIC (2)
                Animal(
                    "gp_8",
                    "American Bison",
                    "Thundering prairie giant",
                    Rarity.RARE,
                    "Great Plains",
                    "Once numbering in the millions, bison shaped grasslands from Texas to the Dakotas. Hooves churned soil and seeded wildflowers. Restored herds now anchor a rebounding prairie story."
                ),
                Animal(
                    "gp_9",
                    "Pronghorn",
                    "Plains speedster",
                    Rarity.RARE,
                    "Great Plains",
                    "North America’s fastest land mammal sprints near 60 mph, built for big horizons. Huge eyes scan for coyotes across open sage. Ancient migration routes still thread the High Plains."
                ),
                // LEGENDARY (1)
                Animal(
                    "gp_10",
                    "Whooping Crane",
                    "Towering white migrant",
                    Rarity.LEGENDARY,
                    "Great Plains",
                    "Among the world’s rarest birds, whoopers stop on Nebraska’s Platte River during epic migrations. At five feet tall, they stand eye-to-eye with deer. Stringent protections shepherd every family south and back again."
                )
            )
        ),
        Region(
            "Far North",
            listOf(
                // COMMON (3)
                Animal(
                    "north_1",
                    "Snowshoe Hare",
                    "Seasonally white rabbit",
                    Rarity.COMMON,
                    "Far North",
                    "Color-shifting coats flip from brown to white as snow arrives. Oversized feet act like built-in snowshoes. Their boom-and-bust cycles ripple through northern food webs."
                ),
                Animal(
                    "north_2",
                    "Common Raven",
                    "Clever arctic corvid",
                    Rarity.COMMON,
                    "Far North",
                    "Tool-using tricksters that follow wolves and people for scraps. Their calls range from croaks to water-droplet plinks. Pairs often stay together for life across the tundra."
                ),
                Animal(
                    "north_3",
                    "Harbor Seal",
                    "Coastal pinniped",
                    Rarity.COMMON,
                    "Far North",
                    "Whiskered divers that can nap underwater by shutting down half their brain. Pups recognize mom’s call in crowded haul-outs. Ice floes and kelp beds are floating nurseries."
                ),
                // UNCOMMON (2)
                Animal(
                    "north_4",
                    "Arctic Fox",
                    "Snow-white nomad",
                    Rarity.COMMON,
                    "Far North",
                    "Winter coats turn ghost-white, summer coats brown to match tundra seasons. When lemmings boom, fox families balloon too. They trail polar bears to scavenge leftovers on sea ice."
                ),
                Animal(
                    "north_5",
                    "King Eider",
                    "Arctic jewel duck",
                    Rarity.UNCOMMON,
                    "Far North",
                    "Males wear kaleidoscope bills and mint-green necks that look painted on. Huge flocks raft off Alaska in spring. They dive deep for mussels in frigid currents without a shiver."
                ),
                // RARE (2)
                Animal(
                    "north_6",
                    "Muskox",
                    "Shaggy Ice Age relic",
                    Rarity.UNCOMMON,
                    "Far North",
                    "When danger looms, herds form a circle with calves in the center like a living fortress. Qiviut underwool is softer and warmer than cashmere. Tundra winds rattle their guard hairs like bead curtains."
                ),
                Animal(
                    "north_7",
                    "Snowy Owl",
                    "Silent tundra sentinel",
                    Rarity.UNCOMMON,
                    "Far North",
                    "Ghost-white owls scan for voles from drifted hummocks. Some winters they irrupt far south, stunning beachgoers and city birders alike. Feathered feet are down booties against razor wind."
                ),
                // EPIC (2)
                Animal(
                    "north_8",
                    "Moose",
                    "Giant of boreal forests",
                    Rarity.RARE,
                    "Far North",
                    "Standing seven feet at the shoulder, moose wade into icy lakes for aquatic plants. They can swim for miles and dive to lake bottoms. Bull antlers can span six feet like living fences."
                ),
                Animal(
                    "north_9",
                    "Brown Bear (Coastal Grizzly)",
                    "Salmon-powered omnivore",
                    Rarity.RARE,
                    "Far North",
                    "From Katmai to Kodiak, coastal bears grow enormous on salmon runs and sedge meadows. Cubs learn fishing like a family trade. A single tide can mean twenty fish for a skilled bear."
                ),
                // LEGENDARY (1)
                Animal(
                    "north_10",
                    "Polar Bear",
                    "Sea-ice specialist",
                    Rarity.LEGENDARY,
                    "Far North",
                    "The Arctic’s top predator patrols pack ice along Alaska’s North Slope. It can scent a seal’s breathing hole from miles away. Transparent fur over black skin turns sunlight into stealthy warmth."
                )
            )
        )
    )
}