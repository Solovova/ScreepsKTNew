package constants

import accounts.initBody
import accounts.initHead
import mainContext.MainContext
import mainContext.messenger
import screeps.api.*
import screeps.utils.unsafe.delete

class Constants(val mainContext: MainContext) {
    val globalConstant: GlobalConstant = GlobalConstant(this)  //cashed
    var mainRoomsInit: Array<String> = arrayOf() //simple
    var mainRooms: Array<String> = arrayOf() //simple after check mainRoomsInit
    val mainRoomConstantContainer: MutableMap<String, MainRoomConstant> = mutableMapOf() //cashed

    var battleGroups: Array<String> = arrayOf() //cashed
    val battleGroupConstantContainer: MutableMap<String, BattleGroupConstant> = mutableMapOf() //cashed
    val battleGroupContainerConstant: BattleGroupContainerConstant = BattleGroupContainerConstant() //cashed

    init {
        this.initHead()
        this.fromMemory()
        this.initBody()
    }

    fun initMainRoomConstantContainer(names: Array<String>) {
        this.mainRoomsInit = names
        var resultMainRooms: Array<String> = arrayOf()
        for (name in names)
            if (Game.rooms[name] != null && (Game.rooms[name]?.controller?.my == true)) {
                mainRoomConstantContainer[name] = MainRoomConstant(this)
                resultMainRooms += name
            } else mainContext.messenger("ERROR", name, "initialization don't see room in Game.rooms", COLOR_RED)
        this.mainRooms = resultMainRooms
    }

    fun getMainRoomConstant(mainRoomName: String): MainRoomConstant {
        val mainRoomConstant: MainRoomConstant? = mainRoomConstantContainer[mainRoomName]
        return if (mainRoomConstant == null) {
            mainContext.messenger("ERROR", mainRoomName, "initialization don't see MainRoomConstant", COLOR_RED)
            MainRoomConstant(this)
        } else mainRoomConstant
    }

    fun m(index: Int): MainRoomConstant {
        if (index >= this.mainRoomsInit.size) {
            mainContext.messenger("ERROR", "$index", "initialization M out of range main room", COLOR_RED)
            return MainRoomConstant(this)
        }
        return this.getMainRoomConstant(this.mainRoomsInit[index])
    }

    fun s(indexMain: Int, indexSlave: Int): SlaveRoomConstant {
        if (indexMain >= this.mainRoomsInit.size) {
            mainContext.messenger("ERROR", "$indexMain", "initialization S out of range main room", COLOR_RED)
            return SlaveRoomConstant()
        }
        val mainRoomConstant: MainRoomConstant = this.getMainRoomConstant(this.mainRoomsInit[indexMain])
        return mainRoomConstant.s(indexSlave)
    }

    private fun toDynamic(): dynamic {
        val result: dynamic = object {}
        result["globalConstant"] = this.globalConstant.toDynamic()
        result["mainRoomConstantContainer"] = object {}
        for (record in this.mainRoomConstantContainer)
            result["mainRoomConstantContainer"][record.key] = record.value.toDynamic()

        try {
            result["battleGroups"] = battleGroups
            result["battleGroupConstantContainer"] = object {}
            for (record in this.battleGroupConstantContainer)
                result["battleGroupConstantContainer"][record.key] = record.value.toDynamic()
            result["battleGroupContainerConstant"] = this.battleGroupContainerConstant.toDynamic()
        } catch (e: Exception) {
            mainContext.messenger("ERROR", "Battle group to dynamic", "", COLOR_RED)
        }

        return result
    }

    fun toMemory() {
        val d: dynamic = this.toDynamic()
        delete(Memory["global"])
        Memory["global"] = d
    }

    private fun fromDynamic(d: dynamic) {
        if (d["mainRoomConstantContainer"] != null)
            for (record in mainRoomConstantContainer)
                record.value.fromDynamic(d["mainRoomConstantContainer"][record.key])

        if (d["globalConstant"] != null)
            globalConstant.fromDynamic(d["globalConstant"])

        try {
            if ((d["battleGroups"] != null))
                this.battleGroups = d["battleGroups"] as Array<String>


            for (record in this.battleGroups) {
                this.battleGroupConstantContainer[record] = BattleGroupConstant()
                if (d["battleGroupConstantContainer"] != null && d["battleGroupConstantContainer"][record] != null)
                    this.battleGroupConstantContainer[record]?.fromDynamic(d["battleGroupConstantContainer"][record])
            }

            if (d["battleGroupContainerConstant"] != null)
                battleGroupContainerConstant.fromDynamic(d["battleGroupContainerConstant"])
        } catch (e: Exception) {
            mainContext.messenger("ERROR", "Battle group from dynamic", "", COLOR_RED)
        }
    }

    private fun fromMemory() {
        val d: dynamic = Memory["global"]
        if (d != null) this.fromDynamic(d)
    }
}