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

        val isInitialized = prefs.getBoolean("app_initialized", false)

        if (!isInitialized) {
            // First time initialization - clear everything
            prefs.edit().clear().apply()
            stepPrefs.edit().clear().apply()

            // Mark as initialized
            prefs.edit().putBoolean("app_initialized", true).apply()
            stepPrefs.edit().putBoolean("first_launch_complete", false).apply()

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
    fun addLure(count: Int = 1) {
        val currentLures = getLureCount()
        setLureCount(currentLures + count)
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
                    "Salvelinus fontinalis",
                    Rarity.COMMON,
                    "The Appalachians",
                    "The only native trout across much of Appalachia, glowing with blue halos and pumpkin fins. It needs icy, ultra-clean streams—if brookies thrive, your water’s healthy. Anglers call them the jewels of mountain creeks."
                ),
                Animal(
                    "app_2",
                    "American Black Bear",
                    "Ursus americanus",
                    Rarity.COMMON,
                    "The Appalachians",
                    "Expert climbers that can sprint 30 mph, black bears roam oak ridges and berry thickets. Their noses are so keen they can smell food miles away. Most encounters end with a huff and a retreat."
                ),
                Animal(
                    "app_3",
                    "Ruffed Grouse",
                    "Bonasa umbellus",
                    Rarity.COMMON,
                    "The Appalachians",
                    "Males “drum” by beating their wings on logs, a heartbeat thudding through spring hollows. Populations cycle with young forest growth. In deep winter they plunge into powder to sleep warmly under the snow."
                ),
                // UNCOMMON (2)
                Animal(
                    "app_4",
                    "Timber Rattlesnake",
                    "Crotalus horridus",
                    Rarity.COMMON, // <- rarity from your code
                    "The Appalachians",
                    "Velvet-black tails and banded coils blend into leaf litter on sun-warmed outcrops. They overwinter in communal dens for decades. Calm by nature, most rattles are warnings—step back and everyone’s happy."
                ),
                Animal(
                    "app_5",
                    "Appalachian Cottontail",
                    "Sylvilagus obscurus",
                    Rarity.UNCOMMON,
                    "The Appalachians",
                    "A secretive rabbit of cool, brushy mountaintops, often misidentified as its common cousin. DNA and subtle field marks tell it apart. Finding one means you’ve climbed into true high-country habitat."
                ),
                // RARE (2)
                Animal(
                    "app_6",
                    "Hellbender",
                    "Cryptobranchus alleganiensis",
                    Rarity.UNCOMMON, // <- rarity from your code
                    "The Appalachians",
                    "North America’s largest fully aquatic salamander hides under boulders in cold, fast rivers. It “breathes” through skin folds that need oxygen-rich water. Clean streams mean healthy hellbenders."
                ),
                Animal(
                    "app_7",
                    "Cerulean Warbler",
                    "Setophaga cerulea",
                    Rarity.UNCOMMON, // <- rarity from your code
                    "The Appalachians",
                    "This tiny migrant nests high in mature forest canopies, a buzzy trill floating from ridge to ridge. Habitat loss hit it hard, making intact Appalachians vital. Spotting one feels like catching a scrap of sky."
                ),
                // EPIC (2)
                Animal(
                    "app_8",
                    "Virginia Big-eared Bat",
                    "Corynorhinus townsendii virginianus",
                    Rarity.RARE, // <- rarity from your code
                    "The Appalachians",
                    "Endangered bats that roost in cool caves and cliffline crevices. Their oversized ears swivel to home in on faint insect buzzes. Quiet caves and dark summer foraging routes keep colonies alive."
                ),
                Animal(
                    "app_9",
                    "Cheat Mountain Salamander",
                    "Plethodon nettingi",
                    Rarity.RARE, // <- rarity from your code
                    "The Appalachians",
                    "Restricted to a handful of spruce-capped ridges in West Virginia. Lungless and moisture-loving, it absorbs oxygen through its skin. The species’ entire world fits within a few mountaintops."
                ),
                // LEGENDARY (1)
                Animal(
                    "app_10",
                    "Spruce-fir Moss Spider",
                    "Microhexura montivaga",
                    Rarity.LEGENDARY,
                    "The Appalachians",
                    "Smaller than a fingernail and clinging to moss mats on the highest summits. Dependent on cool, shaded rock amid red spruce and Fraser fir. Warming peaks put this Appalachian original on a razor’s edge."
                )
            )
        ),
        Region(
            "Mojave Desert",
            listOf(
                // COMMON (4)
                Animal(
                    "moj_1",
                    "Merriam’s Kangaroo Rat",
                    "Dipodomys merriami",
                    Rarity.COMMON,
                    "Mojave Desert",
                    "This tiny nocturnal rodent survives without ever drinking water. Powerful back legs launch it in zig-zag leaps while cheek pouches carry seed harvests home like saddlebags."
                ),
                Animal(
                    "moj_2",
                    "Black-tailed Jackrabbit",
                    "Lepus californicus",
                    Rarity.COMMON,
                    "Mojave Desert",
                    "Built for desert speed, jackrabbits can sprint over 40 mph to escape coyotes and hawks. Their enormous ears work like cooling towers, radiating excess heat into the dry air. By crouching motionless in the shade of shrubs, they nearly disappear against the sandy ground—until they explode into a blur of long leaps across the desert floor."
                ),
                Animal(
                    "moj_3",
                    "Desert Cottontail",
                    "Sylvilagus audubonii",
                    Rarity.COMMON,
                    "Mojave Desert",
                    "One of the Mojave’s most familiar residents, the desert cottontail thrives in creosote flats and rocky washes. Its diet shifts with the seasons, from tender spring grasses to woody shrubs in drought. When startled, it flashes its white cotton-tail as a decoy, zigzagging unpredictably to outmaneuver hungry bobcats, coyotes, and raptors that patrol the desert."
                ),
                Animal(
                    "moj_4",
                    "Greater Roadrunner",
                    "Geococcyx californianus",
                    Rarity.COMMON,
                    "Mojave Desert",
                    "A desert icon, the roadrunner combines the speed of a sprinter with the cunning of a predator. Capable of running faster than most humans, it dashes across open flats to snap up lizards, scorpions, and even rattlesnakes with lightning-fast strikes. Mornings begin with wings spread to soak up the sun’s warmth, before it resumes its patrol of the desert scrub."
                ),
                Animal(
                    "moj_5",
                    "Desert Tortoise",
                    "Gopherus agassizii",
                    Rarity.UNCOMMON,
                    "Mojave Desert",
                    "This slow-moving reptile is a master of survival in a land of extremes. Spending up to 95% of its life in underground burrows, it avoids both blistering summer heat and freezing winter nights. After rare rains, it gulps down water and stores it in its bladder like a living canteen. Once widespread, it is now threatened and a symbol of Mojave conservation."
                ),
                Animal(
                    "moj_6",
                    "LeConte’s Thrasher",
                    "Toxostoma lecontei",
                    Rarity.UNCOMMON,
                    "Mojave Desert",
                    "A true desert specialist, this pale, sand-colored songbird blends seamlessly into dunes and washes. Unlike many birds, it runs more often than it flies, sprinting low over the sand to dig insects and seeds with its long, curved bill. Its haunting calls echo across the Mojave, but catching a glimpse of it requires patience and sharp eyes."
                ),
                Animal(
                    "moj_7",
                    "Burrowing Owl",
                    "Athene cunicularia",
                    Rarity.UNCOMMON,
                    "Mojave Desert",
                    "Unlike most owls, the burrowing owl is active in daylight, standing guard at the entrances of old rodent burrows. With long legs for sprinting, it bobs its head at intruders before vanishing underground. At dusk, it emerges to sweep silently across the desert floor, snapping up grasshoppers, beetles, and small mammals under the fading Mojave sun."
                ),
                // RARE (2)
                Animal(
                    "moj_8",
                    "Chuckwalla",
                    "Sauromalus ater",
                    Rarity.RARE,
                    "Mojave Desert",
                    "A heavy-bodied, plant-eating lizard that basks like a statue on sunlit rocks. When threatened, it scurries into a crevice and inflates its lungs to wedge tightly, making it nearly impossible to pull free. Its calm demeanor and habit of soaking in heat for hours at a time make it one of the Mojave’s most iconic reptiles."
                ),
                Animal(
                    "moj_9",
                    "Mohave Ground Squirrel",
                    "Xerospermophilus mohavensis",
                    Rarity.RARE,
                    "Mojave Desert",
                    "Found only in the Mojave Desert, this squirrel is both elusive and threatened. It vanishes underground for months during drought, making sightings rare. Its entire global range fits within one desert—true Mojave treasure."
                ),
                // LEGENDARY (1)
                Animal(
                    "moj_10",
                    "Mojave Desert Sidewinder",
                    "Crotalus cerastes cerastes",
                    Rarity.LEGENDARY,
                    "Mojave Desert",
                    "The Sidewinder rattlesnake is an emblem of desert mastery, moving sideways across shifting dunes where others sink. Its J-shaped tracks mark its passage through the sand. Raised horn-like scales above its eyes shield against grit, giving it a fierce appearance. Though small, its venom packs a punch, and its elusive nature makes an encounter feel like crossing paths with a ghost of the Mojave."
                )
            )
        ),
        Region(
            "Northern Rockies",
            listOf(
                // COMMON (4)
                Animal(
                    "pnw_1",
                    "Columbian Ground Squirrel",
                    "Urocitellus columbianus",
                    Rarity.COMMON,
                    "Northern Rockies",
                    "These burrowing squirrels are abundant in mountain meadows. They spend nearly 8 months of the year hibernating underground, then emerge in spring to whistle alarms and graze on lush alpine grasses."
                ),
                Animal(
                    "pnw_2",
                    "Rocky Mountain Elk",
                    "Cervus canadensis nelsoni",
                    Rarity.COMMON,
                    "Northern Rockies",
                    "Herds of elk bugle haunting calls across valleys each fall. Males battle with towering antlers, while their migration routes shape predator movements and forest health in the Rockies."
                ),
                Animal(
                    "pnw_3",
                    "Yellow-bellied Marmot",
                    "Marmota flaviventris",
                    Rarity.COMMON,
                    "Northern Rockies",
                    "These burly alpine rodents sunbathe on boulders by day and whistle loudly when danger approaches. Their long hibernation—up to 8 months—helps them survive snowy winters."
                ),
                Animal(
                    "pnw_4",
                    "Dusky Grouse",
                    "Dendragapus obscurus",
                    Rarity.COMMON,
                    "Northern Rockies",
                    "This large forest grouse thrives in high-elevation conifer stands. Males perform booming chest displays in spring, a sound echoing through quiet mountain ridges."
                ),

                // UNCOMMON (3)
                Animal(
                    "pnw_5",
                    "Rocky Mountain Goat",
                    "Oreamnos americanus",
                    Rarity.UNCOMMON,
                    "Northern Rockies",
                    "Sure-footed mountaineers, these shaggy white goats navigate cliffs no predator dares follow. Their thick coats and specialized hooves allow them to thrive on icy, windswept peaks."
                ),
                Animal(
                    "pnw_6",
                    "Pine Marten",
                    "Martes americana",
                    Rarity.UNCOMMON,
                    "Northern Rockies",
                    "A sleek, cat-like hunter of dense conifer forests. Pine martens weave through branches in pursuit of squirrels and birds, their presence signaling healthy, old-growth woods."
                ),
                Animal(
                    "pnw_7",
                    "Harlequin Duck",
                    "Histrionicus histrionicus",
                    Rarity.UNCOMMON,
                    "Northern Rockies",
                    "Males wear striking blue, chestnut, and white plumage. They breed only along cold, rushing mountain streams—making them a rare and dazzling sight in the Rockies."
                ),

                // RARE (2)
                Animal(
                    "pnw_8",
                    "Wolverine",
                    "Gulo gulo",
                    Rarity.RARE,
                    "Northern Rockies",
                    "Solitary, powerful, and elusive, the wolverine roams enormous alpine territories. Few animals embody wild resilience like this tenacious predator, able to drive off wolves and survive blizzards."
                ),
                Animal(
                    "pnw_9",
                    "Canada Lynx",
                    "Lynx canadensis",
                    Rarity.RARE,
                    "Northern Rockies",
                    "With enormous snowshoe-like paws, the lynx silently stalks snowshoe hares in deep winter forests. Populations here are among the southernmost strongholds of this elusive northern cat."
                ),

                // LEGENDARY (1)
                Animal(
                    "pnw_10",
                    "Grizzly Bear",
                    "Ursus arctos horribilis",
                    Rarity.LEGENDARY,
                    "Northern Rockies",
                    "Towering and formidable, the grizzly symbolizes wilderness itself. Once nearly eliminated, it persists in rugged Northern Rockies strongholds. From salmon streams to alpine berry patches, its presence shapes entire ecosystems."
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
                    "Cynomys ludovicianus",
                    Rarity.COMMON,
                    "Great Plains",
                    "Prairie dogs engineer sprawling towns with air-conditioned tunnels and lookout mounds. Their chirps form a complex alarm language that can describe predators. Whole ecosystems revolve around their digging."
                ),
                Animal(
                    "gp_2",
                    "Western Meadowlark",
                    "Sturnella neglecta",
                    Rarity.COMMON,
                    "Great Plains",
                    "A liquid, flute-like song pours from fenceposts after spring storms. Bright breast chevrons flash when it hops through bluestem. State bird in multiple Plains states for good reason."
                ),
                Animal(
                    "gp_3",
                    "Ornate Box Turtle",
                    "Terrapene ornata",
                    Rarity.COMMON,
                    "Great Plains",
                    "High-domed shells wear yellow sunbursts on dark backgrounds like portable prairies. They burrow to escape heat and drought. Road crossings remain their toughest modern gauntlet."
                ),
                // UNCOMMON (2)
                Animal(
                    "gp_4",
                    "Burrowing Owl",
                    "Athene cunicularia",
                    Rarity.COMMON,
                    "Great Plains",
                    "Long legs and bright eyes give a look of perpetual surprise. It nests in abandoned prairie dog tunnels and bobs its head at intruders. Dusk patrols skim like brown bumblebees over grass."
                ),
                Animal(
                    "gp_5",
                    "American Badger",
                    "Taxidea taxus",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "Shovel-like claws and a low gear make badgers excavation pros. They flip sod for ground squirrels and sometimes even team up with coyotes. Fresh fans of dirt mark their work sites."
                ),
                // RARE (2)
                Animal(
                    "gp_6",
                    "Swift Fox",
                    "Vulpes velox",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "Barely cat-sized, swift foxes streak between sage and bluestem at freeway speeds for their size. They den on slight rises to scan for danger. Reintroductions are repopulating old haunts."
                ),
                Animal(
                    "gp_7",
                    "Greater Prairie-Chicken",
                    "Tympanuchus cupido",
                    Rarity.UNCOMMON,
                    "Great Plains",
                    "At dawn, males inflate orange neck sacs and stamp their feet in a rattling blur. Leks are grassland stages passed down for generations. Without tallgrass, the dance falls silent."
                ),
                // EPIC (2)
                Animal(
                    "gp_8",
                    "American Bison",
                    "Bison bison",
                    Rarity.RARE,
                    "Great Plains",
                    "Once numbering in the millions, bison shaped grasslands from Texas to the Dakotas. Hooves churned soil and seeded wildflowers. Restored herds now anchor a rebounding prairie story."
                ),
                Animal(
                    "gp_9",
                    "Pronghorn",
                    "Antilocapra americana",
                    Rarity.RARE,
                    "Great Plains",
                    "North America’s fastest land mammal sprints near 60 mph, built for big horizons. Huge eyes scan for coyotes across open sage. Ancient migration routes still thread the High Plains."
                ),
                // LEGENDARY (1)
                Animal(
                    "gp_10",
                    "Whooping Crane",
                    "Grus americana",
                    Rarity.LEGENDARY,
                    "Great Plains",
                    "Among the world’s rarest birds, whoopers stop on Nebraska’s Platte River during epic migrations. At five feet tall, they stand eye-to-eye with deer. Stringent protections shepherd every family south and back again."
                )
            )
        ),
        Region(
            "Everglades",
            listOf(
                // COMMON (4)
                Animal(
                    "ev_1",
                    "American Alligator",
                    "Alligator mississippiensis",
                    Rarity.COMMON,
                    "Everglades",
                    "The keystone of the Everglades, alligators dig 'gator holes' that hold water in drought, sheltering fish, turtles, and birds. Bellowing males make the marsh vibrate like a drum each spring."
                ),
                Animal(
                    "ev_2",
                    "West Indian Manatee",
                    "Trichechus manatus",
                    Rarity.COMMON,
                    "Everglades",
                    "Gentle 'sea cows' cruise slow waters and graze on seagrass beds. Mothers nurse calves for two years, guiding them through warm river mouths and coastal lagoons."
                ),
                Animal(
                    "ev_3",
                    "Great Egret",
                    "Ardea alba",
                    Rarity.COMMON,
                    "Everglades",
                    "Tall, white wading birds that spear fish with lightning thrusts. Once nearly hunted to extinction for their plumes, they now stand as graceful symbols of wetland recovery."
                ),
                Animal(
                    "ev_4",
                    "Florida Softshell Turtle",
                    "Apalone ferox",
                    Rarity.COMMON,
                    "Everglades",
                    "Flat, pancake-like shells and snorkel snouts make this turtle perfectly adapted for shallow swamps. It buries in mud with only eyes and nose exposed, ambushing fish and frogs."
                ),

                // UNCOMMON (3)
                Animal(
                    "ev_5",
                    "Roseate Spoonbill",
                    "Platalea ajaja",
                    Rarity.UNCOMMON,
                    "Everglades",
                    "Brilliant pink plumage rivals flamingos, yet its spatula bill is its true marvel—sweeping side to side to sift shrimp and minnows. Their colonies light up the marsh like sunrise clouds."
                ),
                Animal(
                    "ev_6",
                    "Snail Kite",
                    "Rostrhamus sociabilis plumbeus",
                    Rarity.UNCOMMON,
                    "Everglades",
                    "A raptor found in the U.S. almost only here. Its sharply hooked bill pries open apple snails with surgical precision. Its fate is tied to the water levels that govern snail survival."
                ),
                Animal(
                    "ev_7",
                    "Limpkin",
                    "Aramus guarauna",
                    Rarity.UNCOMMON,
                    "Everglades",
                    "Their wailing cries echo over wetlands at night. Limpkins stalk marsh edges, cracking open apple snails with persistence that inspired their haunting local nickname, 'crying bird.'"
                ),

                // RARE (2)
                Animal(
                    "ev_8",
                    "Florida Panther",
                    "Puma concolor coryi",
                    Rarity.RARE,
                    "Everglades",
                    "This critically endangered cougar subspecies survives only in South Florida. Elusive and nocturnal, fewer than 200 remain, roaming cypress swamps and pinelands under cover of darkness."
                ),
                Animal(
                    "ev_9",
                    "American Crocodile",
                    "Crocodylus acutus",
                    Rarity.RARE,
                    "Everglades",
                    "Unlike its alligator cousins, this crocodile prefers brackish estuaries and coastal mangroves. Narrow snouts, lighter color, and tolerance for saltwater set it apart in shared waters."
                ),

                // LEGENDARY (1)
                Animal(
                    "ev_10",
                    "Everglades Snail Kite",
                    "Rostrhamus sociabilis plumbeus",
                    Rarity.LEGENDARY,
                    "Everglades",
                    "This raptor is more than rare—it is the Everglades’ survival story. Entirely dependent on apple snails and stable marsh water, its presence is a fragile barometer of the ecosystem’s health. Spotting one in flight feels like witnessing the fate of the wetlands embodied in wings."
                )
            )
        )
    )
}