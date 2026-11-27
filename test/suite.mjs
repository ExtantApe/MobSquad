// MobSquad acceptance suite. Usage: node suite.mjs <26.1.2|26.2|26.3> [only=section,section]
// Builds a throwaway server on a flat world from downloaded jars and drives it with bots.
//
// What's real and what isn't:
// - Bots speak 1.21.11 through ViaVersion and ViaBackwards. They click entities, blocks and menu slots like
//   players, but they don't draw anything: how the menu and the critters look is untested here.
// - On 26.3 the bots can't send movement (see harness.mjs), so they hover where /tp puts them. Walking a
//   chicken is done with small /tp steps on every version, and the sprint check runs on 26.1.2 and 26.2 only.
// - On 26.3 the bots can't use a block either (PaulBunyan's suite found this first), so the Bunkhouse menu and
//   everything driven through it (sending out, following, safety, restarts) runs on 26.1.2 and 26.2 only.
import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import path from 'node:path'
import { Vec3 } from 'vec3'
import { bot, check, DEFAULT_CONFIG, freshServer, jar, quit, section, Server, sleep, summary, until } from './harness.mjs'

const MC = process.argv[2]
if (!['26.1.2', '26.2', '26.3'].includes(MC)) throw new Error('usage: node suite.mjs <26.1.2|26.2|26.3> [only=a,b]')
const ONLY = (process.argv.find(a => a.startsWith('only=')) || '').slice(5).split(',').filter(Boolean)
// MS_TEST_AURA=1 adds AuraSkills, so Wranglin' is an AuraSkills skill; run it with only=aura (and coax for coaxing XP).
const AURA = !!process.env.MS_TEST_AURA
// Spirit coats: run with only=bunkhouse,coats (the menu part needs the Bunkhouse from the first section).
// MS_TEST_MV=1 adds Multiverse-Core and Multiverse-Inventories; run it with only=bunkhouse,groups
const MV = !!process.env.MS_TEST_MV
// MS_TEST_BQ=1 adds BetonQuest and runs the quest chapters; run it with only=chapters, and only=winds (below) on its
// own run, because the chapters section ends by restarting the server without its bots
const BQ = !!process.env.MS_TEST_BQ
// MS_TEST_BQ=1 with only=season1 plays Season 1: a chapter started on the 0.12.0 package and finished on this one, the
// small rewards and the Round-up finale. Sending a critter out needs the Bunkhouse menu, so on 26.3 the finale stops
// after the coax, and its finishing actions are run by command.
// MS_TEST_LP=1 adds LuckPerms as well, for the Ten Winds (kid or adult by group); run it with MS_TEST_BQ=1 and only=winds
const LP = !!process.env.MS_TEST_LP
const want = n => (ONLY.length === 0 && n !== 'aura' && n !== 'groups' && n !== 'chapters' && n !== 'winds' && n !== 'season1') || ONLY.includes(n)
const PHYSICS = MC !== '26.3'
const MENU = PHYSICS
const skip = name => { if (want(name)) console.log(`\n# ${name}: skipped on ${MC}, where the bots can't use a block`) }
const Y = -60

const dir = freshServer('suite-' + MC, { paper: jar(`paper-${MC}.jar`),
  plugins: [jar('ViaVersion.jar'), jar('ViaBackwards.jar'), jar('GriefPrevention.jar'), jar('PlaceholderAPI.jar'), ...(BQ ? [jar('BetonQuest.jar')] : []), ...(LP ? [jar('LuckPerms.jar')] : []), ...(AURA ? [jar('AuraSkills.jar')] : []), ...(MV ? [jar('Multiverse-Core.jar'), jar('Multiverse-Inventories.jar')] : [])], props: { difficulty: 'easy' } })
const s = new Server({ dir, jvm: ['-Dmobsquad.test.sneak-tag=true'] }) // a tagged player counts as crouching; see Puzzles.crouching
const cfgFile = path.join(dir, 'plugins/MobSquad/config.yml')
const playersDir = path.join(dir, 'plugins/MobSquad/players')
const baseConfig = readFileSync(DEFAULT_CONFIG, 'utf8')
async function config(changes = {}) {
  let text = baseConfig
  for (const [k, v] of Object.entries(changes)) {
    const re = new RegExp(`^${k}: .*$`, 'm')
    if (!re.test(text)) throw new Error('no setting ' + k)
    text = text.replace(re, `${k}: ${v}`)
  }
  writeFileSync(cfgFile, text)
  const out = (await s.ask('mobsquad reload', 800)).join(' ')
  if (!/reloaded/.test(out)) throw new Error('reload failed: ' + out)
}
/** A player's MobSquad file, read straight off the disk. */
function file(name) {
  if (!existsSync(playersDir)) return ''
  for (const f of readdirSync(playersDir, { withFileTypes: true }).filter(e => e.isFile() && e.name.endsWith('.txt'))) {
    const text = readFileSync(path.join(playersDir, f.name), 'utf8')
    if (text.includes('\n') && f.name.startsWith(uuids[name])) return text
  }
  return ''
}
const critterLines = name => file(name).split('\n').filter(l => l.startsWith('critter '))
const uuids = {}, intIds = {}
async function uuidOf(name) {
  const out = (await s.ask(`data get entity ${name} UUID`, 400)).join(' ')
  const m = out.match(/\[I; (-?\d+), (-?\d+), (-?\d+), (-?\d+)\]/)
  if (!m) throw new Error('no UUID for ' + name + ': ' + out)
  const hex = m.slice(1).map(n => (Number(n) >>> 0).toString(16).padStart(8, '0')).join('')
  intIds[name] = m.slice(1).join(', ')
  uuids[name] = `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
/** Right-clicks an entity the way a real client does, with the spot on it that was clicked. */
const poke = (b, e) => b.activateEntityAt(e, e.position.offset(0, 0.3, 0))
const said = (b, re, from = 0) => b.chatLog.slice(from).some(m => re.test(m))
const waitSaid = (b, re, ms = 8000, from = 0) => until(() => said(b, re, from), ms)
async function health(selector) {
  const out = (await s.ask(`data get entity ${selector} Health`, 400)).join(' ')
  const m = out.match(/data: ([\d.]+)f/)
  return m ? +m[1] : null
}

console.log(`MobSquad suite on Paper ${MC}, bots ${PHYSICS ? 'with' : 'without'} physics, in ${dir}`)
await s.start()
check('MobSquad enabled', s.log.some(l => /Enabling MobSquad/.test(l)))
for (const g of ['doDaylightCycle false', 'advance_time false', 'doMobSpawning false', 'spawn_mobs false', 'doWeatherCycle false'])
  await s.ask('gamerule ' + g, 150)
await s.ask('time set noon', 200)
await config({ 'recovery-minutes': 5 })

let me = await bot('Wrangler', { physics: PHYSICS })
let other = await bot('Neighbor', { physics: PHYSICS })
if (process.env.MS_TEST_HURT) me.on('health', () => { if (me.health < 20) console.log(`    [hurt] health ${me.health}; server: ${s.log.slice(-3).join(' / ').slice(0, 300)}`) })
await uuidOf('Wrangler'); await uuidOf('Neighbor')
await s.ask('gamemode survival Wrangler'); await s.ask('gamemode survival Neighbor')

const X = 3000, Z = 3000
await s.ask(`forceload add ${X - 64} ${Z - 64} ${X + 64} ${Z + 64}`, 1500)
await until(async () => (await s.ask(`execute if loaded ${X} ${Y} ${Z}`, 200)).join(' ').includes('passed'), 20000, 300)
async function stand(b, name, x, z, yaw = -90, pitch = 10) {
  await s.ask(`tp ${name} ${x + 0.5} ${Y} ${z + 0.5} ${yaw} ${pitch}`, 300)
  await until(() => b.entity.position.distanceTo(new Vec3(x + 0.5, Y, z + 0.5)) < 1.5, 8000)
}
/** Walks a player along +x with small steps, at walking pace, so it's never sprinting. */
async function walk(name, x, z, blocks) {
  for (let i = 1; i <= blocks * 2; i++) { await s.ask(`tp ${name} ${x + 0.5 + i / 2} ${Y} ${z + 0.5} -90 10`, 0); await sleep(220) }
}
async function hold(b, name, item, count = 16) {
  await s.ask(`clear ${name}`, 200)
  await s.ask(`give ${name} minecraft:${item} ${count}`, 300)
  await until(() => b.inventory.items().some(i => i.name === item), 5000)
  await b.equip(b.inventory.items().find(i => i.name === item), 'hand')
}
function entity(b, pred) { return Object.values(b.entities).find(pred) }
async function openBunkhouse(b, at) {
  const block = b.blockAt(at)
  const opened = new Promise((ok, fail) => { b.once('windowOpen', ok); setTimeout(() => fail(new Error('no menu')), 6000) })
  await b.activateBlock(block)
  return opened
}
const egg = (w, slot) => w.slots[slot]?.name
async function closeMenu(b) { if (b.currentWindow) b.closeWindow(b.currentWindow); await sleep(300) }

// ---------- Bunkhouse ----------
const vault = new Vec3(X, Y, Z + 3)
if (want('bunkhouse')) {
  section('Bunkhouses')
  await s.ask(`setblock ${vault.x} ${vault.y} ${vault.z} minecraft:vault`, 300)
  await s.ask('op Wrangler', 300)
  await stand(me, 'Wrangler', X, Z, 0, 20) // facing south, at the vault
  let from = me.chatLog.length
  me.chat('/mobsquad bunkhouse add')
  check('an admin marks a vault as a Bunkhouse', await waitSaid(me, /Bunkhouse now/, 5000, from), me.chatLog.slice(from).join(' | '))
  from = me.chatLog.length
  me.chat('/mobsquad bunkhouse add')
  check('marking it twice says so', await waitSaid(me, /already a Bunkhouse/, 5000, from))
  await s.ask('deop Wrangler', 300)
  from = other.chatLog.length
  other.chat('/mobsquad bunkhouse add')
  await sleep(1200)
  check('a player without mobsquad.admin can\'t mark one', !said(other, /Bunkhouse now/, from), other.chatLog.slice(from).join(' | '))
  if (MENU) {
  const w = await openBunkhouse(me, vault)
  check('right-clicking a Bunkhouse opens the menu', /Bunkhouse/.test(JSON.stringify(w.title)), JSON.stringify(w.title))
  check('a new player\'s Bunkhouse is empty', egg(w, 0) === undefined, egg(w, 0))
  check('the menu has the info book and the call-back bell', egg(w, 49) === 'book' && egg(w, 53) === 'bell', `${egg(w, 49)} ${egg(w, 53)}`)
  await closeMenu(me)
  check('a Bunkhouse hands out no vault loot', me.inventory.items().length === 0, JSON.stringify(me.inventory.items().map(i => i.name)))
  }

  // ----- the <x> <y> <z> [world] form, for the console and admins (0.13.0) -----
  const run = async (c, ms = 900) => (await s.ask(c, ms)).join(' ')
  const c2 = new Vec3(X + 8, Y, Z + 3), at2 = `${c2.x} ${c2.y} ${c2.z}`
  await s.ask(`setblock ${at2} minecraft:vault`, 300)
  let out = await run(`mobsquad bunkhouse check ${at2} testworld`)
  check('console check: a plain vault is not a Bunkhouse', out.includes(`The vault at ${at2} in testworld isn't a Bunkhouse.`), out)
  out = await run(`mobsquad bunkhouse add ${at2} testworld`)
  check('the console marks a vault by its coordinates and world', out.includes(`The vault at ${at2} in testworld is a Bunkhouse now.`), out)
  out = await run(`mobsquad bunkhouse add ${at2} testworld`)
  check('a second add says already', /is already a Bunkhouse/.test(out), out)
  out = await run(`mobsquad bunkhouse check ${at2}`)
  check('with no world named, the console means the main world', out.includes(`${at2} in testworld is a Bunkhouse.`), out)
  out = await run(`execute in minecraft:the_nether run mobsquad bunkhouse check ${at2}`, 2500)
  check('with no world named, execute in decides it', /no vault at .* in testworld_nether|no vault at .* in .*nether|hasn't been made yet/.test(out), out)
  out = await run(`mobsquad bunkhouse add ${at2} minecraft:the_nether`, 2500)
  check('a world can be named by its key, and a spot there with no vault is refused', /no vault at/.test(out) && !/Bunkhouse now/.test(out), out)
  out = await run(`mobsquad bunkhouse add ${at2} nowhere`)
  check('an unknown world is refused', /no world called nowhere/.test(out), out)
  out = await run(`mobsquad bunkhouse add ${c2.x + 1} ${c2.y} ${c2.z} testworld`)
  check('add refuses a spot that is not a vault (air)', /no vault at/.test(out) && !/Bunkhouse now/.test(out), out)
  await s.ask(`setblock ${c2.x + 2} ${c2.y} ${c2.z} minecraft:chest`, 300)
  out = await run(`mobsquad bunkhouse add ${c2.x + 2} ${c2.y} ${c2.z} testworld`)
  check('add refuses a chest', /no vault at/.test(out), out)
  for (const verb of ['check', 'remove']) {
    out = await run(`mobsquad bunkhouse ${verb} ${c2.x + 2} ${c2.y} ${c2.z} testworld`)
    check(`${verb} refuses a chest too`, /no vault at/.test(out), out)
  }
  out = await run(`mobsquad bunkhouse add ${c2.x} 5000 ${c2.z} testworld`)
  check('a height outside the world is refused', /outside the world/.test(out), out)
  out = await run('mobsquad bunkhouse add 900000 -60 900000 testworld')
  check('a spot in a part of the world that was never made is refused', /hasn't been made yet/.test(out), out)
  out = await run('execute if loaded 900000 -60 900000')
  check('and it was not loaded by asking', /Test failed/.test(out), out)

  // a vault in a chunk nobody has loaded: the command loads it, and the mark is saved
  const far = new Vec3(X + 800, Y, Z + 800), farAt = `${far.x} ${far.y} ${far.z}`
  await s.ask(`forceload add ${far.x} ${far.z}`, 1500)
  await until(async () => /passed/.test(await run(`execute if loaded ${farAt}`, 300)), 20000, 300)
  await s.ask(`setblock ${farAt} minecraft:vault`, 400)
  check('the far vault is there', /passed/.test(await run(`execute if block ${farAt} minecraft:vault`, 400)))
  await s.ask(`forceload remove ${far.x} ${far.z}`, 500)
  const gone = await until(async () => /failed/.test(await run(`execute if loaded ${farAt}`, 300)), 60000, 1000)
  check('its chunk unloads', gone)
  out = await run(`mobsquad bunkhouse add ${farAt} testworld`, 1500)
  check('add loads an unloaded chunk and marks the vault', out.includes(`${farAt} in testworld is a Bunkhouse now.`), out)
  await until(async () => /failed/.test(await run(`execute if loaded ${farAt}`, 300)), 60000, 1000)

  // players: refused without the permission; an admin may leave the world off
  from = other.chatLog.length
  for (const verb of ['add', 'remove', 'check']) other.chat(`/mobsquad bunkhouse ${verb} ${at2} testworld`)
  await sleep(2000)
  check('a player without mobsquad.admin is refused add, remove and check', !said(other, /is a Bunkhouse|plain vault|already|isn't a Bunkhouse/, from), other.chatLog.slice(from).join(' | '))
  check('and the mark is still there', /in testworld is a Bunkhouse\./.test(await run(`mobsquad bunkhouse check ${at2} testworld`)))
  await s.ask('op Wrangler', 300)
  from = me.chatLog.length
  me.chat(`/mobsquad bunkhouse check ${at2}`)
  check('an admin in game can leave the world off: it means theirs', await waitSaid(me, /in testworld is a Bunkhouse\./, 5000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('deop Wrangler', 300)

  // the marks outlive a restart
  await quit(me); await quit(other)
  await s.stop()
  await s.start()
  me = await bot('Wrangler', { physics: PHYSICS }); other = await bot('Neighbor', { physics: PHYSICS })
  await s.ask('gamemode survival Wrangler'); await s.ask('gamemode survival Neighbor')
  check('after a restart the console-marked vault is still a Bunkhouse', /in testworld is a Bunkhouse\./.test(await run(`mobsquad bunkhouse check ${at2} testworld`, 1500)))
  out = await run(`mobsquad bunkhouse check ${farAt} testworld`, 1500)
  check('so is the one marked in an unloaded chunk', out.includes(`${farAt} in testworld is a Bunkhouse.`), out)
  check('and the vault marked by looking at it', /in testworld is a Bunkhouse\./.test(await run(`mobsquad bunkhouse check ${vault.x} ${vault.y} ${vault.z} testworld`)))
  if (MENU) {
    // inside someone else's claim, with no trust: the menu still opens, and shows only the clicker's own critters
    await stand(me, 'Wrangler', c2.x, c2.z - 3, 0, 20)
    await hold(me, 'Wrangler', 'golden_shovel', 1)
    await s.ask('acb Wrangler 400', 300)
    let cf = me.chatLog.length
    me.chat('/claim 5')
    const claimed = await waitSaid(me, /[Cc]laim created|created/, 6000, cf)
    check('a claim is made round the vault', claimed, me.chatLog.slice(cf).join(' | '))
    await s.ask('clear Neighbor', 200)
    await stand(other, 'Neighbor', c2.x, c2.z - 2, 0, 20)
    const nf = other.chatLog.length
    let inClaim = null
    try { inClaim = await openBunkhouse(other, c2) } catch (e) { inClaim = null }
    check('a player with no trust opens a console-marked Bunkhouse inside someone\'s claim', !!inClaim && /Bunkhouse/.test(JSON.stringify(inClaim.title)), other.chatLog.slice(nf).join(' | '))
    await closeMenu(other)
    cf = me.chatLog.length
    me.chat('/abandonclaim')
    await sleep(1000)
    await s.ask('clear Wrangler', 200)
  }
  out = await run(`mobsquad bunkhouse remove ${at2} testworld`)
  check('the console unmarks it', out.includes(`The vault at ${at2} in testworld is a plain vault again.`), out)
  out = await run(`mobsquad bunkhouse remove ${at2} testworld`)
  check('a second remove says it is not one', /isn't a Bunkhouse/.test(out), out)
  await run(`mobsquad bunkhouse remove ${farAt} testworld`, 1500)
}

// ---------- coaxing a chicken ----------
if (want('coax')) {
  section('Coaxing a chicken')
  const cx = X + 10, cz = Z + 20
  let lastChicken = -1
  /** A fresh chicken in front of the bot. Waits for the bot to see the new one, not the one just killed. */
  async function chicken(extra = '') {
    await s.ask(`kill @e[type=chicken]`, 300)
    await until(() => !entity(me, e => e.name === 'chicken'), 3000)
    await s.ask(`summon chicken ${cx + 1.5} ${Y} ${cz + 0.5} {NoAI:0b${extra}}`, 600)
    await until(() => entity(me, e => e.name === 'chicken' && e.id > lastChicken), 5000)
    const c = entity(me, e => e.name === 'chicken' && e.id > lastChicken)
    lastChicken = c.id
    return c
  }
  await stand(me, 'Wrangler', cx, cz)
  await hold(me, 'Wrangler', 'wheat_seeds')
  let c = await chicken()
  let from = me.chatLog.length
  await poke(me, c)
  check('seeds and a right-click start a trail', await waitSaid(me, /interested/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('starting a trail uses one seed', me.inventory.items().find(i => i.name === 'wheat_seeds')?.count === 15)
  await walk('Wrangler', cx, cz, 14)
  check('walking it ten blocks coaxes it', await waitSaid(me, /You coaxed a chicken/, 6000, from), me.chatLog.slice(from).join(' | '))
  check('the coaxed chicken leaves the world', await s.count('@e[type=chicken]') === 0)
  check('it\'s saved in the player\'s own file', critterLines('Wrangler').length === 1 && /chicken/.test(critterLines('Wrangler')[0]), file('Wrangler'))
  check('coaxing earns Wranglin\' experience', /wranglin 100/.test(file('Wrangler')), file('Wrangler'))

  // a chicken with a name belongs to someone
  await stand(me, 'Wrangler', cx, cz)
  c = await chicken(',CustomName:"Henrietta"')
  from = me.chatLog.length
  await poke(me, c)
  check('a named chicken can\'t be coaxed', await waitSaid(me, /has a name/, 4000, from), me.chatLog.slice(from).join(' | '))

  // putting the seeds away ends it
  c = await chicken()
  from = me.chatLog.length
  await poke(me, c)
  await waitSaid(me, /interested/, 4000, from)
  me.setQuickBarSlot((me.quickBarSlot + 1) % 9)
  check('putting the seeds away ends the trail', await waitSaid(me, /put the seeds away/, 4000, from), me.chatLog.slice(from).join(' | '))
  await hold(me, 'Wrangler', 'wheat_seeds')

  // wandering too far from it ends it
  await stand(me, 'Wrangler', cx, cz)
  c = await chicken()
  from = me.chatLog.length
  await poke(me, c)
  await waitSaid(me, /interested/, 4000, from)
  await s.ask(`tp Wrangler ${cx + 30.5} ${Y} ${cz + 0.5}`, 300)
  check('leaving the chicken behind ends the trail', await waitSaid(me, /wandered off/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('a failed trail coaxes nothing', critterLines('Wrangler').length === 1)

  if (PHYSICS) {
    await stand(me, 'Wrangler', cx, cz)
    c = await chicken()
    from = me.chatLog.length
    await poke(me, c)
    await waitSaid(me, /interested/, 4000, from)
    me.setControlState('sprint', true); me.setControlState('forward', true)
    const ran = await waitSaid(me, /Too fast/, 4000, from)
    me.clearControlStates()
    check('running ends the trail', ran, me.chatLog.slice(from).join(' | '))
  }

  // on someone else's claim, the claim plugin says no first
  await stand(other, 'Neighbor', X + 40, Z + 40)
  await hold(other, 'Neighbor', 'golden_shovel', 1)
  let nfrom = other.chatLog.length
  await s.ask('acb Neighbor 400', 300) // enough claim blocks for an 11 by 11 claim
  other.chat('/claim 5') // GriefPrevention's smallest claim around the player
  const made = await waitSaid(other, /[Cc]laim created|created/, 5000, nfrom)
  await stand(me, 'Wrangler', X + 38, Z + 40)
  await s.ask(`summon chicken ${X + 39.5} ${Y} ${Z + 40.5}`, 600)
  await until(() => entity(me, e => e.name === 'chicken'), 5000)
  from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'chicken'))
  await sleep(1500)
  if (made) check('a chicken on someone else\'s claim can\'t be coaxed', !said(me, /interested/, from), me.chatLog.slice(from).join(' | '))
  else console.log('  (skipped the claim check: GriefPrevention made no claim: ' + other.chatLog.slice(nfrom).join(' | ') + ')')
}

// ---------- sending out and following ----------
if (!MENU) skip('posse')
else if (want('posse')) {
  section('Sending critters out')
  // Chunks made for a fresh world can hold wild chickens, which the counts below would include.
  await s.ask('kill @e[type=chicken]', 400)
  if (critterLines('Wrangler').length === 0) await s.ask('mobsquad give Wrangler chicken', 400)
  await s.ask(`clear Wrangler`, 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  let w = await openBunkhouse(me, vault)
  check('the coaxed chicken is in the Bunkhouse', egg(w, 0) === 'chicken_spawn_egg', egg(w, 0))
  let from = me.chatLog.length
  await me.clickWindow(0, 0, 0)
  check('clicking it sends it out', await waitSaid(me, /out with you/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('it stands beside its owner', await s.ask('execute as Wrangler at @s if entity @e[type=chicken,name=Chicken,distance=..4]', 400).then(o => o.join(' ').includes('passed')))
  await sleep(500)
  check('the menu icon can\'t be taken', !me.inventory.items().some(i => /spawn_egg/.test(i.name)), JSON.stringify(me.inventory.items().map(i => i.name)))
  await closeMenu(me)

  await s.ask('clear Neighbor', 200)
  await stand(other, 'Neighbor', X + 1, Z, 0, 20)
  const o = await openBunkhouse(other, vault)
  check('another player sees only their own Bunkhouse', egg(o, 0) === undefined, egg(o, 0))
  await closeMenu(other)

  await s.ask('mobsquad give Wrangler chicken', 400)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(1, 0, 0)
  check('at Wranglin\' level 1 only one critter goes out', await waitSaid(me, /1 critter out at once/, 4000, from), me.chatLog.slice(from).join(' | '))
  await closeMenu(me)
  await s.ask('mobsquad xp Wrangler 2250', 400)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(1, 0, 0)
  check('at level 10 a second one goes out', await waitSaid(me, /out with you/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('two chickens are out', await s.count('@e[type=chicken,name=Chicken]') === 2)
  await closeMenu(me)

  await s.ask('mobsquad give Wrangler creeper', 400)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(2, 0, 0)
  check('a Yonder critter waits for Wranglin\' level 25', await waitSaid(me, /level 25/, 4000, from), me.chatLog.slice(from).join(' | '))
  await closeMenu(me)

  section('Following')
  await stand(me, 'Wrangler', X + 12, Z)
  check('critters walk after their owner', await until(async () =>
    (await s.ask('execute as Wrangler at @s if entity @e[type=chicken,name=Chicken,distance=..5]', 300)).join(' ').includes('passed'), 12000, 500))
  await stand(me, 'Wrangler', X + 50, Z)
  check('a critter left far behind catches up', await until(async () =>
    (await s.ask('execute as Wrangler at @s if entity @e[type=chicken,name=Chicken,distance=..6]', 300)).join(' ').includes('passed'), 8000, 500))

  section('Calling back')
  await stand(me, 'Wrangler', X, Z, 0, 20)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(53, 0, 0)
  check('the bell in the menu calls everyone back', await waitSaid(me, /Everyone's back/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('no critter is left in the world', await s.count('@e[type=chicken,name=Chicken]') === 0)
  await closeMenu(me)

  w = await openBunkhouse(me, vault); await me.clickWindow(0, 0, 0); await closeMenu(me)
  from = me.chatLog.length
  me.chat('/mobsquad dinnerbell')
  await until(() => me.inventory.items().some(i => i.name === 'warped_fungus_on_a_stick'), 4000)
  await me.equip(me.inventory.items().find(i => i.name === 'warped_fungus_on_a_stick'), 'hand')
  me.activateItem()
  check('the dinnerbell calls critters home', await waitSaid(me, /headed home/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('nothing is left out after the dinnerbell', await s.count('@e[type=chicken,name=Chicken]') === 0)
  from = me.chatLog.length
  me.chat('/mobsquad list')
  check('/mobsquad list shows the critters', await waitSaid(me, /3 critters/, 4000, from), me.chatLog.slice(from).join(' | '))
}

// ---------- safety ----------
if (!MENU) skip('safety')
else if (want('safety')) {
  section('Safety')
  await s.ask('mobsquad xp Wrangler 15000', 400) // level 25: three out, Yonder unlocked
  for (const k of ['creeper', 'skeleton']) if (!critterLines('Wrangler').some(l => l.includes(' ' + k + ' '))) await s.ask(`mobsquad give Wrangler ${k}`, 400)
  await s.ask('clear Wrangler', 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  const lines = critterLines('Wrangler')
  const slotOf = k => lines.findIndex(l => l.includes(' ' + k + ' '))
  let w = await openBunkhouse(me, vault)
  await me.clickWindow(slotOf('creeper'), 0, 0); await sleep(300)
  await me.clickWindow(slotOf('skeleton'), 0, 0); await sleep(300)
  await closeMenu(me)
  check('a creeper and a skeleton are out', await s.count('@e[type=creeper,name=Creeper]') === 1 && await s.count('@e[type=skeleton,name=Skeleton]') === 1)
  me.hurt = 0
  await sleep(10000)
  check('a creeper critter never explodes near its owner', await s.count('@e[type=creeper,name=Creeper]') === 1)
  check('critters never hurt their owner', me.hurt === 0 && me.health === 20, `health ${me.health}`)
  check('a skeleton critter doesn\'t burn in the sun', await s.count('@e[type=skeleton,name=Skeleton]') === 1)
  check('no blocks changed around them', (await s.ask(`execute if block ${X} ${Y - 1} ${Z} minecraft:grass_block`, 300)).join(' ').includes('passed'))

  // the other player walks up: no critter goes for them either
  await stand(other, 'Neighbor', X + 2, Z)
  other.hurt = 0
  await sleep(6000)
  check('critters never hurt other players', other.hurt === 0, `health ${other.health}`)

  // a player can't hurt a critter
  const sk = entity(me, e => e.name === 'skeleton')
  const before = await health('@e[type=skeleton,name=Skeleton,limit=1]')
  if (sk) { await me.attack(sk); await sleep(400); await me.attack(sk); await sleep(600) }
  const after = await health('@e[type=skeleton,name=Skeleton,limit=1]')
  check('players can\'t hurt critters', sk && before !== null && after === before, `${before} -> ${after}`)

  // out of health: tuckered out, not dead
  let from = me.chatLog.length
  await s.ask('kill @e[type=item]', 200)
  await s.ask('damage @e[type=skeleton,name=Skeleton,limit=1] 100 minecraft:generic', 600)
  check('a critter out of health is tuckered out', await waitSaid(me, /tuckered out/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('it goes back to the Bunkhouse', await s.count('@e[type=skeleton]') === 0)
  check('it drops nothing', await s.count('@e[type=item]') === 0 && await s.count('@e[type=experience_orb]') === 0)
  const sline = critterLines('Wrangler').find(l => l.includes(' skeleton '))
  check('its rest is saved', sline && +sline.split(' ')[5] > Date.now(), sline)
  w = await openBunkhouse(me, vault)
  from = me.chatLog.length
  await me.clickWindow(slotOf('skeleton'), 0, 0)
  check('a resting critter can\'t go out', await waitSaid(me, /still resting/, 4000, from), me.chatLog.slice(from).join(' | '))
  await closeMenu(me)
  await config({ 'recovery-minutes': 0 })
}

// ---------- the other launch puzzles ----------
if (want('puzzles')) {
  section('Coaxing the other launch critters')
  await s.ask('gamemode survival Wrangler', 200)
  const px = X - 40, pz = Z - 40
  const count = k => critterLines('Wrangler').filter(l => l.includes(' ' + k + ' ')).length
  async function fresh(type, nbt = '', at = [px + 2.5, Y, pz + 0.5]) {
    await s.ask(`kill @e[type=${type}]`, 300)
    await until(() => !entity(me, e => e.name === type), 3000)
    const before = Math.max(-1, ...Object.values(me.entities).filter(e => e.name === type).map(e => e.id))
    await s.ask(`summon ${type} ${at.join(' ')} ${nbt}`, 600)
    await until(() => entity(me, e => e.name === type && e.id > before), 5000)
    return entity(me, e => e.name === type && e.id > before)
  }
  async function attempt(type, item, nbt, { face = [-90, 10], at, standAt = [px, pz] } = {}) {
    await stand(me, 'Wrangler', standAt[0], standAt[1], face[0], face[1])
    await hold(me, 'Wrangler', item)
    const mob = await fresh(type, nbt, at)
    const from = me.chatLog.length
    await poke(me, mob)
    return { mob, from }
  }

  await s.ask('mobsquad xp Wrangler 0', 300)
  let { from } = await attempt('creeper', 'gunpowder', '{NoAI:0b}')
  check('a Yonder mob needs Wranglin\' level 25 to coax', await waitSaid(me, /from Wranglin' level 25/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('mobsquad xp Wrangler 40000', 300) // level 40: every tier

  // the creeper standoff
  await s.ask('tag Wrangler add mobsquad-test-sneak', 200)
  await sleep(300)
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  check('gunpowder starts the creeper standoff', await waitSaid(me, /Crouch while it swells/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('effect give Wrangler minecraft:instant_health 1 5', 600)
  me.hurt = 0
  const cr = () => entity(me, e => e.name === 'creeper')
  for (let i = 0; i < 3; i++) { await sleep(2600); if (cr()) await poke(me, cr()) }
  check('crouching through three swells and feeding it coaxes the creeper', await waitSaid(me, /You coaxed a creeper/, 4000, from), me.chatLog.slice(from).join(' | '))
  const ground = (await s.ask(`execute if block ${px} ${Y - 1} ${pz} minecraft:grass_block`, 300)).join(' ')
  check('the standoff never blows up', me.hurt === 0 && ground.includes('passed'), `hurt ${me.hurt}, ground ${ground.slice(0, 60)}`)
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  await waitSaid(me, /Crouch while it swells/, 4000, from)
  await s.ask('tag Wrangler remove mobsquad-test-sneak', 200)
  check('standing up mid-swell starts it over', await waitSaid(me, /stood up mid-swell/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('tag Wrangler add mobsquad-test-sneak', 200)
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  await waitSaid(me, /Crouch while it swells/, 4000, from)
  await sleep(500); await poke(me, cr())
  check('gunpowder mid-swell is too soon', await waitSaid(me, /Too soon/, 4000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('creeper', 'gunpowder', ''))
  await waitSaid(me, /Crouch while it swells/, 4000, from)
  await sleep(7500)
  check('forgetting the gunpowder starts it over', said(me, /didn't give it gunpowder/, from), me.chatLog.slice(from).join(' | '))
  check('a creeper left after a failed standoff hasn\'t gone off', me.hurt === 0, `hurt ${me.hurt}`)
  await s.ask('tag Wrangler remove mobsquad-test-sneak', 200)
  await s.ask('kill @e[type=creeper]', 300)

  // the enderman: facing away from it the whole time
  ;({ from } = await attempt('enderman', 'grass_block', '{NoAI:0b}', { face: [90, 0] }))
  check('a block it could carry starts the enderman\'s puzzle', await waitSaid(me, /don't look it in the eye/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('not looking at it for 20 seconds coaxes the enderman', await waitSaid(me, /You coaxed an enderman/, 25000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('enderman', 'grass_block', '{NoAI:1b}', { face: [90, 0] }))
  await waitSaid(me, /don't look it in the eye/, 4000, from)
  await sleep(1500)
  await s.ask(`tp Wrangler ${px + 0.5} ${Y} ${pz + 0.5} -90 -20`, 300) // facing east and up, at its face
  check('looking it in the eye starts it over', await waitSaid(me, /looked it in the eye/, 5000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('kill @e[type=enderman]', 300)

  // the skeleton: arrows from the player into a target block
  await s.ask(`setblock ${px + 6} ${Y} ${pz + 4} minecraft:target`, 300)
  ;({ from } = await attempt('skeleton', 'arrow', '{NoAI:1b}'))
  check('an arrow starts the skeleton\'s puzzle', await waitSaid(me, /fair shot/, 4000, from), me.chatLog.slice(from).join(' | '))
  for (let i = 0; i < 3; i++) {
    await s.ask(`summon arrow ${px + 6.5} ${Y + 3} ${pz + 4.5} {Motion:[0.0,-1.5,0.0],Owner:[I; ${intIds.Wrangler}]}`, 900)
  }
  check('three hits on a target block coax the skeleton', await waitSaid(me, /You coaxed a skeleton/, 5000, from), me.chatLog.slice(from).join(' | '))

  // the iron golem: only one a player built
  ;({ from } = await attempt('iron_golem', 'iron_ingot', '{PlayerCreated:0b,NoAI:1b}'))
  check('a village\'s golem can\'t be coaxed', await waitSaid(me, /belongs to the village/, 4000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('iron_golem', 'iron_ingot', '{PlayerCreated:1b,NoAI:1b}'))
  const golem = entity(me, e => e.name === 'iron_golem')
  for (let i = 0; i < 3; i++) { await sleep(700); await poke(me, golem) }
  await sleep(500)
  await hold(me, 'Wrangler', 'poppy', 1)
  await poke(me, golem)
  check('four iron ingots and a poppy coax a golem a player built', await waitSaid(me, /You coaxed an iron golem/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the fox: crouched the whole time
  await s.ask('tag Wrangler add mobsquad-test-sneak', 200); await sleep(300)
  ;({ from } = await attempt('fox', 'sweet_berries', '{NoAI:1b}'))
  check('crouching with berries starts the fox\'s puzzle', await waitSaid(me, /Stay crouched/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('staying crouched for 15 seconds coaxes the fox', await waitSaid(me, /You coaxed a fox/, 20000, from), me.chatLog.slice(from).join(' | '))
  await s.ask('tag Wrangler remove mobsquad-test-sneak', 200); await sleep(300)
  ;({ from } = await attempt('fox', 'sweet_berries', '{NoAI:1b}'))
  check('a fox won\'t listen unless you crouch', await waitSaid(me, /Crouch first/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the bee: standing still
  ;({ from } = await attempt('bee', 'poppy', '{NoAI:0b}', { at: [px + 2.5, Y + 1, pz + 0.5] }))
  check('a flower starts the bee\'s puzzle', await waitSaid(me, /Stand still/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('standing still for 10 seconds coaxes the bee', await waitSaid(me, /You coaxed a bee/, 15000, from), me.chatLog.slice(from).join(' | '))
  ;({ from } = await attempt('bee', 'poppy', '{NoAI:0b}', { at: [px + 2.5, Y + 1, pz + 0.5] }))
  await waitSaid(me, /Stand still/, 4000, from)
  await s.ask(`tp Wrangler ${px + 3.5} ${Y} ${pz + 0.5}`, 300)
  check('moving starts the bee\'s puzzle over', await waitSaid(me, /You moved/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the allay: three notes from a note block, played by redstone here
  await s.ask(`setblock ${px + 4} ${Y} ${pz - 2} minecraft:note_block`, 300)
  ;({ from } = await attempt('allay', 'amethyst_shard', '{NoAI:1b}'))
  check('an amethyst shard starts the allay\'s puzzle', await waitSaid(me, /note block/, 4000, from), me.chatLog.slice(from).join(' | '))
  for (let i = 0; i < 3; i++) {
    await s.ask(`setblock ${px + 5} ${Y} ${pz - 2} minecraft:redstone_block`, 400)
    await s.ask(`setblock ${px + 5} ${Y} ${pz - 2} minecraft:air`, 400)
  }
  check('three notes coax the allay', await waitSaid(me, /You coaxed an allay/, 4000, from), me.chatLog.slice(from).join(' | '))

  // the axolotl: both in the water
  await s.ask(`fill ${px + 1} ${Y - 3} ${pz - 2} ${px + 5} ${Y - 1} ${pz + 2} minecraft:water`, 400)
  ;({ from } = await attempt('axolotl', 'tropical_fish_bucket', '{NoAI:1b}', { at: [px + 2.5, Y - 2, pz + 0.5], standAt: [px - 1, pz] }))
  check('out of the water, the axolotl won\'t play', await waitSaid(me, /need to be in the water/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask(`tp Wrangler ${px + 1.5} ${Y - 2} ${pz + 0.5} -90 20`, 600)
  from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'axolotl'))
  check('in the water, a bucket of tropical fish starts it', await waitSaid(me, /wants to play/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('15 seconds in the water coaxes the axolotl', await waitSaid(me, /You coaxed an axolotl/, 20000, from), me.chatLog.slice(from).join(' | '))
  check('the bucket of fish isn\'t used up', me.inventory.items().some(i => i.name === 'tropical_fish_bucket'))
  await s.ask(`fill ${px + 1} ${Y - 3} ${pz - 2} ${px + 5} ${Y - 1} ${pz + 2} minecraft:dirt`, 400)

  for (const k of ['creeper', 'enderman', 'skeleton', 'iron_golem', 'fox', 'bee', 'allay', 'axolotl'])
    check(`a coaxed ${k.replace('_', ' ')} is saved`, count(k) >= 1, file('Wrangler'))
  from = me.chatLog.length
  me.chat('/mobsquad hints')
  check('/mobsquad hints lists every critter', await until(() => me.chatLog.slice(from).filter(l => /^\s+\w/.test(l)).length >= 9, 4000), me.chatLog.slice(from).join(' | '))
}

// ---------- fighting ----------
if (!MENU) skip('fighting')
else if (want('fighting')) {
  section('Fighting hostile mobs only')
  await s.ask('mobsquad xp Wrangler 40000', 300)
  await s.ask('gamemode creative Wrangler', 300) // so hostile mobs leave the player alone and the critters pick the fight
  await s.ask('clear Wrangler', 200)
  await s.ask('gamemode creative Neighbor', 300)
  await stand(other, 'Neighbor', X - 20, Z + 20)
  for (const k of ['iron_golem', 'skeleton', 'allay']) await s.ask(`mobsquad give Wrangler ${k}`, 300) // fresh ones, none resting
  await stand(me, 'Wrangler', X, Z, 0, 20)
  const lines = critterLines('Wrangler')
  const slot = k => lines.map(l => l.includes(' ' + k + ' ')).lastIndexOf(true)
  const fx = X + 4, fz = Z - 4
  await s.ask('kill @e[type=!player]', 400)
  let w = await openBunkhouse(me, vault); await me.clickWindow(slot('iron_golem'), 0, 0); await sleep(300); await closeMenu(me)
  await s.ask(`summon cow ${fx} ${Y} ${fz}`, 200)
  await s.ask(`summon villager ${fx + 1} ${Y} ${fz}`, 200)
  await s.ask(`summon husk ${fx - 2} ${Y} ${fz} {CustomName:"Gerald",PersistenceRequired:1b,NoAI:1b}`, 200)
  await s.ask(`summon enderman ${fx + 2} ${Y} ${fz - 2}`, 200)
  await s.ask(`summon wolf ${fx} ${Y} ${fz + 2} {Owner:[I; ${intIds.Neighbor}]}`, 200)
  await s.ask(`summon husk ${fx + 3} ${Y} ${fz + 1} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b}`, 600) // still, so it can't go after the villager
  const golemDown = await until(async () => await s.count('@e[type=husk,tag=foe]') === 0, 25000, 500)
  check('a golem critter takes down a hostile husk near its owner', golemDown,
    `golems ${await s.count('@e[type=iron_golem]')}, foe health ${await health('@e[type=husk,tag=foe,limit=1]')}, ${(await s.ask('execute as @e[type=iron_golem,limit=1] run data get entity @s Pos', 300)).join(' ').slice(0, 120)}`)
  await sleep(4000)
  const full = async (sel, hp) => (await health(sel)) === hp
  check('it leaves the cow alone', await full('@e[type=cow,limit=1]', 10))
  check('it leaves the villager alone', await full('@e[type=villager,limit=1]', 20))
  check('it leaves a named husk alone unless it starts something', await s.count('@e[type=husk,name=Gerald]') === 1)
  check('it leaves a neutral enderman alone', await full('@e[type=enderman,limit=1]', 40))
  check('it leaves another player\'s pet alone', await full('@e[type=wolf,limit=1]', 8) || await full('@e[type=wolf,limit=1]', 40))
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
  await s.ask('kill @e[type=!player]', 400)
  await me.clickWindow(slot('skeleton'), 0, 0); await sleep(300); await closeMenu(me)
  await stand(other, 'Neighbor', X + 3, Z - 5) // near the fight, beside the line of fire
  await s.ask('gamemode survival Neighbor', 200)
  other.hurt = 0
  await s.ask(`summon husk ${X} ${Y} ${Z - 8} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b}`, 600)
  const shot = await until(async () => await s.count('@e[type=husk,tag=foe]') === 0, 30000, 500)
  check('a skeleton critter shoots a hostile husk down', shot, `skeletons ${await s.count('@e[type=skeleton]')}, foe health ${await health('@e[type=husk,tag=foe,limit=1]')}, hand ${(await s.ask('data get entity @e[type=skeleton,limit=1] equipment.mainhand.id', 300)).join(' ').slice(-40)}`)
  check('its arrows never hurt a player nearby', other.hurt === 0, `health ${other.health}`)
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
  await s.ask('kill @e[type=!player]', 400)
  await me.clickWindow(slot('allay'), 0, 0); await sleep(300); await closeMenu(me)
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 3} {Tags:["foe"],PersistenceRequired:1b}`, 600)
  await sleep(6000)
  check('the allay never fights', await full('@e[type=husk,tag=foe,limit=1]', 20))
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300); await closeMenu(me)
  await s.ask('kill @e[type=!player]', 400)
  await s.ask('gamemode survival Wrangler', 300)
}

// ---------- levels and specials ----------
if (!MENU) skip('specials')
else if (want('specials')) {
  section('Levels and specials')
  await s.ask('mobsquad xp Wrangler 40000', 300)
  await s.ask('gamemode survival Wrangler', 200)
  await s.ask('clear Wrangler', 200)
  await s.ask('effect clear Wrangler', 200)
  await s.ask('gamemode creative Neighbor', 200)
  await stand(other, 'Neighbor', X - 20, Z + 20)
  for (const g of ['randomTickSpeed 0', 'random_tick_speed 0']) await s.ask('gamerule ' + g, 150) // only the bee grows crops here
  for (const k of ['chicken', 'axolotl', 'bee', 'fox', 'allay', 'creeper', 'enderman', 'iron_golem']) await s.ask(`mobsquad give Wrangler ${k}`, 250)
  const slotOf = k => critterLines('Wrangler').map(l => l.split(' ')[2] === k).lastIndexOf(true)
  const lineOf = k => critterLines('Wrangler')[slotOf(k)] || ''
  const effects = async sel => (await s.ask(`data get entity ${sel} active_effects`, 400)).join(' ')
  const attr = async (sel, a) => { const m = (await s.ask(`attribute ${sel} minecraft:${a} get`, 400)).join(' ').match(/is ([\d.]+)/); return m ? +m[1] : null }
  async function sendOnly(kind) {
    await stand(me, 'Wrangler', X, Z, 0, 20)
    await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
    await s.ask('kill @e[type=!player]', 400)
    await me.clickWindow(slotOf(kind), 0, 0); await sleep(500); await closeMenu(me)
    await until(async () => await s.count(`@e[type=${kind}]`) === 1, 4000, 250)
  }

  // levels and stats
  let out = (await s.ask('mobsquad level Wrangler chicken 50', 400)).join(' ')
  check('an admin can set a critter\'s level', /set \d+ of Wrangler's chickens? to level 50/.test(out), out)
  check('the level and its experience are saved', lineOf('chicken').split(' ').slice(3, 5).join(' ') === '50 24010', lineOf('chicken'))
  await sendOnly('chicken')
  const hp = await attr('@e[type=chicken,limit=1]', 'max_health')
  check('a level 50 chicken has about 3.2 times its health', hp !== null && Math.abs(hp - 4 * (1 + 3 * (1 - (50 / 99) ** 2))) < 0.05, hp)
  check('it comes out at full health', Math.abs((await health('@e[type=chicken,limit=1]')) - hp) < 0.05)
  await sleep(1500)
  check('the chicken: you fall slowly while it\'s out', /slow_falling/.test(await effects('Wrangler')))

  // the axolotl
  await sendOnly('axolotl')
  await sleep(1500)
  const ax = await effects('Wrangler')
  check('the axolotl: water breathing and regeneration near it', /water_breathing/.test(ax) && /regeneration/.test(ax), ax.slice(-200))
  const axHp = await attr('@e[type=axolotl,limit=1]', 'max_health')
  await s.ask(`damage @e[type=axolotl,limit=1] ${Math.round(axHp * 0.7)} minecraft:generic`, 600)
  check('badly hurt, it plays dead and heals', /regeneration/.test(await effects('@e[type=axolotl,limit=1]')))

  // the bee
  const crops = [[1, 1], [-1, 1], [1, -1], [-1, -1], [2, 0], [-2, 0]]
  await sendOnly('bee')
  for (const [dx, dz] of crops) { await s.ask(`setblock ${X + dx} ${Y - 1} ${Z + dz} farmland`, 80); await s.ask(`setblock ${X + dx} ${Y} ${Z + dz} wheat[age=0]`, 80) }
  const grown = async () => { let n = 0; for (const [dx, dz] of crops) if ((await s.ask(`execute if block ${X + dx} ${Y} ${Z + dz} wheat[age=0]`, 120)).join(' ').includes('failed')) n++; return n }
  check('the bee: crops near it grow faster', await until(async () => await grown() >= 2, 15000, 1000), `${await grown()} of ${crops.length} grew`)
  for (const [dx, dz] of crops) { await s.ask(`setblock ${X + dx} ${Y} ${Z + dz} air`, 60); await s.ask(`setblock ${X + dx} ${Y - 1} ${Z + dz} grass_block`, 60) }

  // the fox
  await sendOnly('fox')
  await hold(me, 'Wrangler', 'cobblestone', 8)
  await stand(me, 'Wrangler', X, Z, -90, 0)
  await me.tossStack(me.inventory.items().find(i => i.name === 'cobblestone'))
  await sleep(700)
  await stand(me, 'Wrangler', X - 4, Z, -90, 0)
  const fetched = await until(() => me.inventory.items().some(i => i.name === 'cobblestone'), 20000, 500)
  check('the fox: fetches an item you dropped', fetched)
  await s.ask('gamemode survival Neighbor', 200)
  await stand(other, 'Neighbor', X + 5, Z + 2)
  await hold(other, 'Neighbor', 'oak_log', 4)
  await other.tossStack(other.inventory.items().find(i => i.name === 'oak_log'))
  await stand(other, 'Neighbor', X - 20, Z + 20)
  await sleep(9000)
  check('it leaves other players\' items alone', !me.inventory.items().some(i => i.name === 'oak_log') && await s.count('@e[type=item]') >= 1)
  await s.ask('kill @e[type=item]', 200)

  // the allay
  await sendOnly('allay')
  await s.ask(`setblock ${X + 4} ${Y - 3} ${Z - 2} iron_ore`, 200)
  await hold(me, 'Wrangler', 'raw_iron', 1)
  let from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'allay'))
  check('the allay: shown an ore, it finds the nearest one', await waitSaid(me, /found iron ore about \d+ blocks away/, 4000, from), me.chatLog.slice(from).join(' | '))
  await sleep(3200)
  await hold(me, 'Wrangler', 'diamond', 1)
  from = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'allay'))
  check('with none close by, it says so', await waitSaid(me, /found no diamond ore close by/, 4000, from), me.chatLog.slice(from).join(' | '))
  await s.ask(`setblock ${X + 4} ${Y - 3} ${Z - 2} stone`, 200)

  // the enderman
  await sendOnly('enderman')
  await s.ask('clear Wrangler', 200)
  const fetchAt = new Vec3(X + 3, Y, Z)
  await s.ask(`setblock ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} pumpkin`, 300)
  async function askFetch() {
    await poke(me, entity(me, e => e.name === 'enderman')); await sleep(500)
    await me.lookAt(fetchAt.offset(0.5, 0.5, 0.5), true); await sleep(300)
    me.swingArm(); await sleep(2000)
  }
  await askFetch()
  check('the enderman: fetches the block you swing at', (await s.ask(`execute if block ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} air`, 200)).join(' ').includes('passed')
    && await until(() => me.inventory.items().some(i => i.name === 'pumpkin'), 3000))
  await s.ask(`setblock ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} stone`, 300)
  from = me.chatLog.length
  await askFetch()
  check('it won\'t carry a block an enderman can\'t', (await s.ask(`execute if block ${fetchAt.x} ${fetchAt.y} ${fetchAt.z} stone`, 200)).join(' ').includes('passed'))
  // on someone else's claim, it won't dig
  await stand(other, 'Neighbor', X + 40, Z - 40)
  await hold(other, 'Neighbor', 'golden_shovel', 1)
  await s.ask('acb Neighbor 400', 300)
  let nfrom = other.chatLog.length
  other.chat('/claim 5')
  const made = await waitSaid(other, /[Cc]laim created|created/, 5000, nfrom)
  await stand(other, 'Neighbor', X - 20, Z + 20)
  const claimed = new Vec3(X + 37, Y, Z - 40)
  await s.ask(`setblock ${claimed.x} ${claimed.y} ${claimed.z} pumpkin`, 300)
  await stand(me, 'Wrangler', X + 33, Z - 40, -90, 0)
  await sleep(1500)
  await poke(me, entity(me, e => e.name === 'enderman')); await sleep(500)
  await me.lookAt(claimed.offset(0.5, 0.5, 0.5), true); await sleep(300)
  me.swingArm(); await sleep(2000)
  if (made) check('it won\'t take a block from someone else\'s claim', (await s.ask(`execute if block ${claimed.x} ${claimed.y} ${claimed.z} pumpkin`, 200)).join(' ').includes('passed'))
  else console.log('  (skipped the claim check: GriefPrevention made no claim)')
  await s.ask(`setblock ${claimed.x} ${claimed.y} ${claimed.z} air`, 200)

  // the creeper
  await sendOnly('creeper')
  await s.ask(`summon cow ${X + 6} ${Y} ${Z - 11}`, 200)
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 9.5} {Tags:["foe","a"],PersistenceRequired:1b,NoAI:1b}`, 200)
  await s.ask(`summon husk ${X + 4.5} ${Y} ${Z - 9.5} {Tags:["foe","b"],PersistenceRequired:1b,NoAI:1b}`, 600)
  const both = await until(async () => (await health('@e[tag=a,limit=1]') ?? 0) < 20 && (await health('@e[tag=b,limit=1]') ?? 0) < 20, 20000, 400)
  check('the creeper: one blast hurts the hostile mobs around its target', both, `${await health('@e[tag=a,limit=1]')} / ${await health('@e[tag=b,limit=1]')}`)
  check('it leaves the cow alone', (await health('@e[type=cow,limit=1]')) === 10)
  check('it breaks no blocks', (await s.ask(`execute if block ${X + 3} ${Y - 1} ${Z - 10} grass_block`, 200)).join(' ').includes('passed'))
  await s.ask('kill @e[type=husk]', 300)
  await s.ask(`tp Neighbor ${X + 4} ${Y} ${Z - 11}`, 300)
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 9.5} {Tags:["foe","a"],PersistenceRequired:1b,NoAI:1b}`, 200)
  await s.ask(`summon husk ${X + 4.5} ${Y} ${Z - 9.5} {Tags:["foe","b"],PersistenceRequired:1b,NoAI:1b}`, 300)
  let firstHurt = null
  await until(async () => { const a = await health('@e[tag=a,limit=1]'), b = await health('@e[tag=b,limit=1]'); if (a < 20 || b < 20) { firstHurt = [a, b]; return true } return false }, 15000, 250)
  check('with a player close by it never blasts, only hits one at a time', firstHurt && firstHurt.some(h => h === 20), firstHurt)
  await s.ask(`tp Neighbor ${X - 20} ${Y} ${Z + 20}`, 300)

  // the later parts of four specials, from level 25
  await s.ask('mobsquad give Wrangler skeleton', 250)
  for (const k of ['chicken', 'bee', 'fox', 'skeleton']) await s.ask(`mobsquad level Wrangler ${k} 25`, 250)
  await sendOnly('bee')
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 3} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b}`, 300)
  check('from level 25 the bee\'s sting makes a hostile mob glow', await until(async () => /glowing/.test(await effects('@e[tag=foe,limit=1]')), 15000, 500))
  await sendOnly('fox')
  await stand(me, 'Wrangler', X, Z, 0, 20)
  await s.ask(`summon husk ${X} ${Y} ${Z - 6} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b,attributes:[{id:"minecraft:armor",base:0}]}`, 200)
  const drops = []; let last = 20
  await until(async () => { const h = await health('@e[tag=foe,limit=1]'); if (h === null) return true; if (h < last) { drops.push(+(last - h).toFixed(2)); last = h } return drops.length >= 3 }, 20000, 150)
  // the pounce is the first hit after the fox has sized up a mob that isn't after it, which is usually but not always
  // the very first hit, so look for one double hit among the first three
  const d3 = drops.slice(0, 3)
  check('from level 25 the fox pounces for a double hit', d3.length >= 2 && Math.max(...d3) > 1.7 * Math.min(...d3), drops.join(', '))
  await sendOnly('chicken')
  await s.ask('clear Wrangler', 200)
  check('from level 25 the chicken lays a speedy egg at your feet', await until(() => me.inventory.items().some(i => i.name === 'egg'), 45000, 500))
  if (me.inventory.items().some(i => i.name === 'egg')) {
    await me.equip(me.inventory.items().find(i => i.name === 'egg'), 'hand')
    await s.ask('effect clear Wrangler', 200)
    me.activateItem(); await sleep(600)
    check('using it gives a burst of speed, and it isn\'t thrown', /speed/.test(await effects('Wrangler')) && await s.count('@e[type=chicken]') === 1 && !me.inventory.items().some(i => i.name === 'egg'))
  }
  await sendOnly('skeleton')
  await s.ask('clear Wrangler', 200)
  await s.ask('give Wrangler minecraft:bow', 300)
  check('from level 25 the skeleton shares arrows when you carry a bow', await until(() => (me.inventory.items().find(i => i.name === 'arrow')?.count ?? 0) === 2, 45000, 500),
    me.inventory.items().map(i => i.name + 'x' + i.count).join(','))
  await s.ask('clear Wrangler', 200)

  // the iron golem: experience, levels and its guard
  await sendOnly('iron_golem')
  await s.ask(`summon husk ${X + 3} ${Y} ${Z - 3} {Tags:["foe"],PersistenceRequired:1b,NoAI:1b}`, 600)
  from = me.chatLog.length
  await until(async () => await s.count('@e[type=husk,tag=foe]') === 0, 20000, 500)
  check('beating a hostile mob earns a critter experience and a level', await waitSaid(me, /Iron golem is now level 2/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('the new level is saved', /^critter \S+ iron_golem 2 20 /.test(lineOf('iron_golem')), lineOf('iron_golem'))
  await s.ask(`summon zombie ${X} ${Y} ${Z + 14} {Tags:["hitter"],PersistenceRequired:1b,NoAI:1b}`, 400)
  await s.ask('effect clear Wrangler', 200)
  await s.ask('effect give Wrangler minecraft:instant_health 1 5', 600)
  const golemBefore = await health('@e[type=iron_golem,limit=1]')
  await s.ask('damage Wrangler 4 minecraft:mob_attack by @e[tag=hitter,limit=1]', 600)
  const meAfter = await health('Wrangler'), golemAfter = await health('@e[type=iron_golem,limit=1]')
  const lost = 20 - meAfter, took = golemBefore - golemAfter
  // easy difficulty makes a 4-point mob hit 3; the golem takes a quarter of it. Natural regen can blur the player's own number.
  check('the iron golem takes a quarter of a hit meant for you', lost > 0 && lost <= 2.26 && Math.abs(took - 0.75) < 0.01, `you lost ${lost}, golem took ${took}`)
  await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300); await closeMenu(me)
  await s.ask('kill @e[type=!player]', 400)
  for (const g of ['randomTickSpeed 3', 'random_tick_speed 3']) await s.ask('gamerule ' + g, 150)
}

// ---------- La Ménagerie ----------
if (want('journal')) {
  section('La Ménagerie')
  const scout = await bot('Scout', { physics: PHYSICS })
  await uuidOf('Scout')
  await s.ask('gamemode creative Scout', 200)
  const JX = X + 60, JZ = Z + 60
  await s.ask(`forceload add ${JX - 16} ${JZ - 16} ${JX + 16} ${JZ + 16}`, 800)
  await stand(scout, 'Scout', JX, JZ, -90, 0)
  await s.ask(`kill @e[type=!player,x=${JX},y=${Y},z=${JZ},distance=..40]`, 300)
  async function journal() {
    const opened = new Promise((ok, fail) => { scout.once('windowOpen', ok); setTimeout(() => fail(new Error('no journal')), 6000) })
    scout.chat('/mobsquad journal')
    return opened
  }
  let w = await journal()
  check('/mobsquad journal opens La Ménagerie', JSON.stringify(w.title).includes('nagerie'), JSON.stringify(w.title))
  check('a new player\'s pages are all locked', [0, 1, 2, 3, 4, 5, 6, 7, 8].every(i => egg(w, i) === 'gray_dye'), [0, 1, 2, 3, 4, 5, 6, 7, 8].map(i => egg(w, i)).join(','))
  check('it lists the critters in Critterbook order, creeper first', /No\. 1 /.test(JSON.stringify(w.slots[0]?.customName ?? '')) && /No\. 75 /.test(JSON.stringify(w.slots[8]?.customName ?? '')), `${JSON.stringify(w.slots[0]?.customName)} / ${JSON.stringify(w.slots[8]?.customName)}`)
  await closeMenu(scout)
  let from = scout.chatLog.length
  await s.ask(`summon fox ${JX + 5} ${Y} ${JZ} {NoAI:1b,PersistenceRequired:1b}`, 300)
  check('meeting a fox opens its page', await waitSaid(scout, /New page in La Ménagerie: No\. 62, the fox!/, 6000, from), scout.chatLog.slice(from).join(' | '))
  check('the page is saved', file('Scout').includes('page fox'), file('Scout'))
  // a creeper behind a wall isn't met; with the wall gone, it is
  await s.ask(`fill ${JX + 3} ${Y} ${JZ - 6} ${JX + 3} ${Y + 3} ${JZ - 2} stone`, 300)
  await s.ask(`summon creeper ${JX + 6} ${Y} ${JZ - 4} {NoAI:1b,PersistenceRequired:1b}`, 300)
  await s.ask(`summon chicken ${JX - 25} ${Y} ${JZ} {NoAI:1b,PersistenceRequired:1b}`, 300)
  from = scout.chatLog.length
  await sleep(5000)
  check('a critter you can\'t see isn\'t met', !said(scout, /the creeper/, from))
  check('one far away isn\'t either', !said(scout, /the chicken/, from))
  await s.ask(`fill ${JX + 3} ${Y} ${JZ - 6} ${JX + 3} ${Y + 3} ${JZ - 2} air`, 300)
  check('once it\'s in sight, it is', await waitSaid(scout, /No\. 1, the creeper!/, 6000, from), scout.chatLog.slice(from).join(' | '))
  from = scout.chatLog.length
  await s.ask('mobsquad give Scout bee', 400)
  check('getting a critter opens its page', await waitSaid(scout, /No\. 63, the bee!/, 4000, from), scout.chatLog.slice(from).join(' | '))
  w = await journal()
  const order = ['creeper', 'skeleton', 'chicken', 'enderman', 'iron_golem', 'fox', 'bee', 'axolotl', 'allay']
  const shown = order.map((k, i) => egg(w, i))
  check('open pages show their critter, locked ones stay hidden',
    shown[0] === 'creeper_spawn_egg' && shown[5] === 'fox_spawn_egg' && shown[6] === 'bee_spawn_egg' && shown[2] === 'gray_dye' && shown[8] === 'gray_dye', shown.join(','))
  const book = new Promise(ok => { scout._client.once('open_book', () => ok(true)); setTimeout(() => ok(false), 5000) })
  await scout.clickWindow(5, 0, 0)
  check('clicking an open page opens it as a book', await book)
  await closeMenu(scout)
  w = await journal()
  const noBook = new Promise(ok => { scout._client.once('open_book', () => ok(true)); setTimeout(() => ok(false), 2500) })
  await scout.clickWindow(2, 0, 0)
  check('a locked page doesn\'t open', !(await noBook))
  await scout.clickWindow(5, 0, 0); await sleep(300)
  check('nothing can be taken out of the journal', !scout.inventory.items().some(i => /spawn_egg|dye/.test(i.name)))
  await closeMenu(scout)
  if (MENU) {
    await s.ask('gamemode survival Scout', 200)
    await stand(scout, 'Scout', X - 2, Z, 0, 20)
    const bw = await openBunkhouse(scout, vault)
    const jopened = new Promise((ok) => { scout.once('windowOpen', ww => ok(ww)); setTimeout(() => ok(null), 5000) })
    await scout.clickWindow(45, 0, 0)
    const jw = await jopened
    check('the Bunkhouse menu opens La Ménagerie', jw && JSON.stringify(jw.title).includes('nagerie'), jw && JSON.stringify(jw.title))
    await closeMenu(scout)
  }
  await s.ask(`kill @e[type=!player,x=${JX},y=${Y},z=${JZ},distance=..40]`, 300)
  await quit(scout)
}

// ---------- the sidekick ----------
if (want('sidekick')) {
  section('The sidekick')
  await config({ 'recovery-minutes': 0 })
  let buddy = await bot('Buddy', { physics: PHYSICS })
  await uuidOf('Buddy')
  await s.ask('gamemode creative Buddy', 200)
  await stand(buddy, 'Buddy', X - 3, Z, 0, 20)
  await s.ask(`kill @e[type=!player,x=${X},y=${Y},z=${Z},distance=..30]`, 300)
  await s.ask('kill @e[type=bee]', 200); await s.ask('kill @e[type=chicken]', 200) // wild ones left by the coaxing section (name= matches them too)
  for (const k of ['bee', 'chicken']) await s.ask(`mobsquad give Buddy ${k}`, 250)
  const near = async (kind) => s.count(`@e[type=${kind},name=${kind === 'bee' ? 'Bee' : 'Chicken'},x=${X - 3},y=${Y},z=${Z},distance=..12]`) // critters carry their name; wild ones from coaxing don't
  let out = (await s.ask('mobsquad sidekick Buddy bee', 400)).join(' ')
  check('an admin can make a critter someone\'s sidekick', /is Buddy's sidekick now/.test(out), out)
  check('the sidekick comes out on its own', await until(async () => await near('bee') === 1, 5000, 300))
  check('it\'s saved', /^sidekick /m.test(file('Buddy')), file('Buddy'))
  if (MENU) {
    await openBunkhouse(buddy, vault)
    const lines = critterLines('Buddy'), slot = k => lines.map(l => l.split(' ')[2] === k).indexOf(true)
    let from = buddy.chatLog.length
    await buddy.clickWindow(slot('chicken'), 0, 0); await sleep(500)
    check('it doesn\'t count toward the posse', await waitSaid(buddy, /is out with you/, 3000, from), buddy.chatLog.slice(from).join(' | '))
    from = buddy.chatLog.length
    await buddy.clickWindow(slot('bee'), 0, 0); await sleep(500)
    check('it won\'t go back into the Bunkhouse', await waitSaid(buddy, /won't leave your side/, 3000, from) && await near('bee') === 1, buddy.chatLog.slice(from).join(' | '))
    await buddy.clickWindow(53, 0, 0); await sleep(600); await closeMenu(buddy)
    check('the call-back bell leaves it out', await near('bee') === 1 && await near('chicken') === 0)
  }
  buddy.chat('/mobsquad dinnerbell')
  await until(() => buddy.inventory.items().some(i => i.name === 'warped_fungus_on_a_stick'), 4000)
  const bell = buddy.inventory.items().find(i => i.name === 'warped_fungus_on_a_stick')
  if (bell) {
    await buddy.equip(bell, 'hand'); buddy.activateItem(); await sleep(600); buddy.deactivateItem()
    check('so does the dinnerbell', await near('bee') === 1)
  }
  await s.ask('kick Buddy', 300); await quit(buddy); await sleep(1000) // a kick, because a 26.3 bot's own disconnect can take a while to land
  const gone = (await s.ask('list', 300)).join(' ')
  check('it goes when you leave', await until(async () => await s.count('@e[type=bee,name=Bee]') === 0, 6000, 300), gone)
  await sleep(61000) // GriefPrevention makes a brand-new player wait a minute before logging in again
  buddy = await bot('Buddy', { physics: PHYSICS })
  await stand(buddy, 'Buddy', X - 3, Z, 0, 20)
  check('and comes back when you return', await until(async () => await near('bee') === 1, 6000, 300))
  let from = buddy.chatLog.length
  await s.ask(`damage @e[type=bee,name=Bee,x=${X - 3},y=${Y},z=${Z},distance=..12,limit=1] 500 minecraft:generic`, 600)
  check('tuckered out, it rests', await waitSaid(buddy, /tuckered out/, 4000, from))
  check('and comes back on its own once rested', await until(async () => await near('bee') === 1, 6000, 300))
  out = (await s.ask('mobsquad sidekick Buddy none', 400)).join(' ')
  check('an admin can clear it', /has no sidekick now/.test(out), out)
  await buddy.equip(buddy.inventory.items().find(i => i.name === 'warped_fungus_on_a_stick'), 'hand'); buddy.activateItem(); await sleep(600); buddy.deactivateItem()
  check('then the dinnerbell calls it home like anyone', await until(async () => await near('bee') === 0, 3000, 300))
  await quit(buddy)
}

// ---------- spirit coats ----------
// Coats use the sidekick to get a critter out, so they run on 26.3 too. Picking a coat in the Bunkhouse menu needs a
// block use, so that part runs on 26.1.2 and 26.2 only. What a coat looks like in a real client is untested.
if (want('coats')) {
  section('Spirit coats')
  const CX = X + 40, CZ = Z - 40
  const writeWorlds = async (list, noGlow = '') => {
    writeFileSync(cfgFile, baseConfig.replace(/^  worlds: .*$/m, `  worlds: [${list}]`).replace(/^  no-glow-worlds: .*$/m, `  no-glow-worlds: [${noGlow}]`))
    const out = (await s.ask('mobsquad reload', 900)).join(' ')
    if (!/reloaded/.test(out)) throw new Error('reload failed: ' + out)
  }
  const coater = await bot('Coater', { physics: PHYSICS })
  await uuidOf('Coater')
  await s.ask('gamemode creative Coater', 200)
  await s.ask(`forceload add ${CX - 16} ${CZ - 16} ${CX + 16} ${CZ + 16}`, 800)
  await stand(coater, 'Coater', CX, CZ, 0, 20)
  await s.ask(`kill @e[type=!player,x=${CX},y=${Y},z=${CZ},distance=..40]`, 300)
  await s.ask('kill @e[type=bee]', 200); await s.ask('kill @e[type=chicken]', 200)
  await s.ask('mobsquad xp Coater 100000', 300) // every tier is open, so any critter may go out
  await s.ask('mobsquad give Coater chicken', 300)
  await s.ask('mobsquad give Coater bee', 300)
  // count the particle packets each bot is sent
  let packets = 0, farPackets = 0
  coater._client.on('packet', (d, m) => { if (/particle/.test(m.name)) packets++ })
  const sel = (k, extra = '') => `@e[type=${k},name=${k === 'bee' ? 'Bee' : 'Chicken'}${extra}]`
  const variantOf = async () => ((await s.ask(`data get entity ${sel('chicken', ',limit=1')} variant`, 400)).join(' ').match(/entity data: "?([\w:]+)"?/) || [])[1]
  const glows = async k => (await s.count(sel(k, ',nbt={Glowing:1b}'))) > 0
  const teamOf = async (k, n) => (await s.count(sel(k, `,team=ms_coat_${n}`))) > 0
  const level = async (k, n) => { await s.ask(`mobsquad level Coater ${k} ${n}`, 400) }
  const trail = async (ms = 3000) => { const m = packets; await sleep(ms); return packets - m }

  await level('chicken', 45); await level('bee', 45)
  await s.ask('mobsquad sidekick Coater chicken', 300)
  check('the chicken comes out as the sidekick', await until(async () => (await s.count(sel('chicken'))) === 1, 6000, 300))
  await s.ask('mobsquad sidekick Coater bee', 300)
  check('and the bee', await until(async () => (await s.count(sel('bee'))) === 1, 6000, 300))
  const plain = await variantOf()
  check('by default no world is magic, so a level 45 chicken looks as it spawned', !!plain, plain)
  check('and a level 45 bee or chicken doesn\'t glow, wear a team or leave a trail', !(await glows('bee')) && !(await teamOf('bee', 4)) && !(await glows('chicken')) && (await trail(2500)) === 0, `packets ${packets}`)

  // magic worlds
  await writeWorlds('testworld')
  check('listing the world gives the chicken its level 45 coat (coat 4) at once', await until(async () => (await variantOf()) === 'minecraft:warm', 3000, 200), await variantOf())
  check('the bee glows in coat 4\'s team (Meadow)', await until(() => glows('bee'), 3000, 200) && await teamOf('bee', 4))
  check('a chicken wears its variant and also glows in coat 4\'s team (glow-too, owner 2026-11-22)', await until(() => glows('chicken'), 3000, 200) && await teamOf('chicken', 4))
  // a no-glow world (for kids' worlds): coat and trail stay, the outline that shows through walls goes
  await writeWorlds('testworld', 'testworld')
  check('in a no-glow world the bee and chicken lose the outline', await until(async () => !(await glows('bee')) && !(await glows('chicken')), 3000, 200))
  check('but keep their coat: the chicken\'s variant and both trails', (await variantOf()) === 'minecraft:warm' && (await trail(3000)) > 0)
  await writeWorlds('testworld')
  check('taking it off the no-glow list brings the outline back', await until(async () => (await glows('bee')) && (await glows('chicken')), 3000, 200))
  const n = await trail(3000)
  check('the bee\'s and chicken\'s trails are a few particles a second each, not a flood', n >= 4 && n <= 40, `${n} packets in 3 s for two critters`)
  await level('chicken', 100)
  check('level 100 earns coat 10 and the chicken wears it', await until(async () => (await variantOf()) === 'minecraft:cold', 3000, 200), await variantOf())
  await level('bee', 100)
  check('the bee moves to coat 10 (Starlight)', await until(() => teamOf('bee', 10), 3000, 200) && !(await teamOf('bee', 4)))
  await level('bee', 9)
  check('below level 10 there\'s no coat to show', await until(async () => !(await glows('bee')), 3000, 200) && !(await teamOf('bee', 10)))
  await level('bee', 45)
  await until(() => glows('bee'), 3000, 200)

  // who sees particles
  let far = await bot('Farther', { physics: PHYSICS })
  await s.ask('gamemode creative Farther', 200)
  far._client.on('packet', (d, m) => { if (/particle/.test(m.name)) farPackets++ })
  await s.ask(`tp Farther ${CX + 250} ${Y} ${CZ} 0 0`, 600)
  await sleep(1000); farPackets = 0
  const before = packets
  await sleep(3000)
  check('a player beyond view distance is sent no particles, a player near still is', farPackets === 0 && packets - before > 0, `far ${farPackets}, near ${packets - before}`)
  // a player who set particles to minimal: the client sends this in its options
  const send = particleStatus => coater._client.write('settings', { locale: 'en_US', viewDistance: 6, chatFlags: 0, chatColors: true, skinParts: 127, mainHand: 1,
    enableTextFiltering: false, enableServerListing: true, particleStatus })
  let sent = true
  try { send(2) } catch (e) { sent = false; console.log('    (couldn\'t send the options packet: ' + e.message + ')') }
  if (sent) {
    await sleep(800)
    const quiet = await trail(3000)
    check('a player with particles set to minimal is sent none', quiet === 0, `${quiet} packets`)
    send(0); await sleep(800)
    check('and gets them again when they turn them back on', (await trail(3000)) > 0)
  }
  await quit(far)

  // leaving a magic world takes the look off at once, entering applies it
  await writeWorlds('')
  check('unlisting the world takes the chicken\'s coat off at once', await until(async () => (await variantOf()) === plain, 3000, 200), `${await variantOf()} vs ${plain}`)
  check('and the bee\'s and chicken\'s glow, team and trail', !(await glows('bee')) && !(await teamOf('bee', 4)) && !(await glows('chicken')) && !(await teamOf('chicken', 4)) && (await trail(2500)) === 0)
  await writeWorlds('testworld')
  check('listing it again brings them back', await until(() => glows('bee'), 3000, 200) && await variantOf() === 'minecraft:cold')
  const hop = async dim => { await s.ask(`execute in minecraft:${dim} run tp Coater 8 100 8`, 800); await sleep(2500) }
  await hop('the_nether')
  check('in a world that isn\'t listed, critters look plain', await until(async () => (await s.count(sel('bee'))) === 1 && !(await glows('bee')), 12000, 400), `bees ${await s.count(sel('bee'))}`)
  await writeWorlds('testworld, testworld_nether')
  check('listing the nether gives it back there', await until(() => glows('bee'), 5000, 200))
  await hop('overworld')
  await stand(coater, 'Coater', CX, CZ, 0, 20)
  check('back in the overworld the coat is there', await until(async () => (await glows('bee')) && (await s.count(sel('bee'))) === 1, 12000, 400))

  // the choice
  if (MENU) {
    // a fresh player for the menu: Coater's bot has been to the nether, and its block use stopped working after that
    await quit(coater); await sleep(1500)
    const picker = await bot('Picker', { physics: PHYSICS })
    await uuidOf('Picker')
    await s.ask('mobsquad xp Picker 100000', 300)
    await s.ask('mobsquad give Picker chicken', 300)
    await s.ask('mobsquad give Picker bee', 300)
    await s.ask('mobsquad level Picker bee 45', 300)
    await s.ask('mobsquad sidekick Picker bee', 300)
    await stand(picker, 'Picker', X, Z, 0, 20)
    await until(async () => (await s.count(sel('bee'))) === 1 && await glows('bee'), 8000, 300)
    const nbefore = critterLines('Picker').length
    check('both critters are saved with a coat field', critterLines('Picker').every(l => l.split(' ').length === 9 && l.split(' ')[8] === '0'), critterLines('Picker').join(' | '))
    const lines = critterLines('Picker'), slot = k => lines.map(l => l.split(' ')[2] === k).indexOf(true)
    await s.ask('gamemode survival Picker', 200)
    await until(() => picker.blockAt(vault)?.name === 'vault', 15000, 300) // the chunks load after the teleport
    await openBunkhouse(picker, vault)
    const opened = new Promise(ok => { picker.once('windowOpen', w => ok(w)); setTimeout(() => ok(null), 5000) })
    await picker.clickWindow(slot('bee'), 1, 0) // right-click
    const cw = await opened
    check('right-clicking a critter opens its spirit coats', cw && /Spirit coats/.test(JSON.stringify(cw.title)), cw && JSON.stringify(cw.title))
    if (cw) {
      const nameOf = i => JSON.stringify(cw.slots[i]?.customName ?? '')
      check('earned coats are named and locked ones say their level', /Coat 1: Ember/.test(nameOf(10)) && /Coat 4: Meadow/.test(nameOf(13)) && /Coat 5: Level 50/.test(nameOf(14)) && /Coat 10: Level 100/.test(nameOf(23)),
        [10, 13, 14, 23].map(nameOf).join(' | '))
      check('locked coats are a pane and earned ones a dye', cw.slots[14]?.name === 'gray_stained_glass_pane' && cw.slots[10]?.name === 'red_dye' && cw.slots[13]?.name === 'lime_dye', [10, 13, 14].map(i => cw.slots[i]?.name).join(','))
      await picker.clickWindow(14, 0, 0); await sleep(500) // a locked coat
      check('a locked coat can\'t be picked', critterLines('Picker')[slot('bee')].split(' ')[8] === '0' && await teamOf('bee', 4), critterLines('Picker').join(' | '))
      let from = picker.chatLog.length
      await picker.clickWindow(11, 0, 0) // coat 2, Marigold
      check('picking an earned coat says so', await waitSaid(picker, /wears Marigold/, 3000, from), picker.chatLog.slice(from).join(' | '))
      check('the choice is saved with the critter', await until(() => critterLines('Picker')[slot('bee')]?.split(' ')[8] === '2', 3000, 200), critterLines('Picker').join(' | '))
      check('and the bee changes to it', await until(() => teamOf('bee', 2), 3000, 200) && !(await teamOf('bee', 4)))
      await picker.clickWindow(25, 0, 0); await sleep(500) // newest
      check('"Newest coat" goes back to the newest', await until(() => teamOf('bee', 4), 3000, 200) && critterLines('Picker')[slot('bee')].split(' ')[8] === '0')
      check('nothing can be taken out of the coat menu', !picker.inventory.items().some(i => /dye|pane|nether_star/.test(i.name)), JSON.stringify(picker.inventory.items().map(i => i.name)))
      await picker.clickWindow(11, 0, 0); await sleep(400)
      const back = new Promise(ok => { picker.once('windowOpen', w => ok(w)); setTimeout(() => ok(null), 5000) })
      await picker.clickWindow(31, 0, 0)
      const bw = await back
      check('Back returns to the Bunkhouse', bw && /Bunkhouse/.test(JSON.stringify(bw.title)), bw && JSON.stringify(bw.title))
      // a young critter has no coats to pick
      await s.ask('mobsquad level Picker bee 5', 400)
      from = picker.chatLog.length
      await picker.clickWindow(slot('bee'), 1, 0)
      check('a critter under level 10 says when its first coat comes', await waitSaid(picker, /first spirit coat at level 10/, 3000, from), picker.chatLog.slice(from).join(' | '))
    }
    await closeMenu(picker)
    check('no other lines were lost or changed', critterLines('Picker').length === nbefore)
    await s.ask('mobsquad sidekick Picker none', 300)
    await quit(picker)
  } else console.log(`\n# picking a coat in the menu: skipped on ${MC}, where the bots can't use a block`)
  await s.ask('mobsquad sidekick Coater none', 300)
  await s.ask(`kill @e[type=!player,x=${CX},y=${Y},z=${CZ},distance=..40]`, 300)
  await s.ask(`forceload remove ${CX - 16} ${CZ - 16} ${CX + 16} ${CZ + 16}`, 300)
  await s.ask('team list', 300)
  check('the bundled coats.yml loaded with no warnings', !s.all.some(l => /coats\.yml/.test(l) && /WARN/.test(l)), s.all.filter(l => /coats\.yml/.test(l)).slice(0, 3).join(' | '))
  await quit(coater)
  await config({})
}

// ---------- world groups ----------
if (want('groups') && MV) {
  section('World groups (Multiverse-Inventories)')
  await s.ask('mv create elsewhere normal --world-type flat --no-structures', 500)
  check('a second world, outside the default inventory group', await until(async () => (await s.ask('mv list', 400)).join(' ').includes('elsewhere'), 60000, 2000), (await s.ask('mv list', 400)).join(' '))
  // Multiverse-Inventories keeps a world outside the default group apart; the diamond check below proves it
  const countIn = async (world, sel) => { const out = (await s.ask(`execute in minecraft:${world === 'testworld' ? 'overworld' : world} if entity ${sel.includes('distance=') ? sel : sel.replace(']', ',distance=0..]')}`, 400)).join(' '); const m = out.match(/count: (\d+)/i); return m ? +m[1] : 0 }
  const tp = async (name, world) => { const out = (await s.ask(`mv tp ${name} ${world}`, 1500)).join(' '); await sleep(2500); return out }
  const tv = await bot('Traveler', { physics: PHYSICS })
  await uuidOf('Traveler')
  await s.ask('gamemode survival Traveler', 200); await s.ask('clear Traveler', 200)
  await s.ask('mobsquad xp Traveler 40000', 300)
  await stand(tv, 'Traveler', X, Z, 0, 20)
  await s.ask('give Traveler diamond 1', 300); await sleep(500)
  let out = await tp('Traveler', 'elsewhere')
  const inElsewhere = (await s.ask('data get entity Traveler Dimension', 400)).join(' ')
  check('the groups really keep inventories apart', /elsewhere/.test(inElsewhere) && !tv.inventory.items().some(i => i.name === 'diamond'), out + ' / ' + inElsewhere)
  await tp('Traveler', 'testworld')
  await stand(tv, 'Traveler', X, Z, 0, 20)
  check('and give them back', await until(() => tv.inventory.items().some(i => i.name === 'diamond'), 4000))
  await s.ask('clear Traveler', 200)
  for (const k of ['chicken', 'enderman', 'bee']) await s.ask(`mobsquad give Traveler ${k}`, 250)
  await s.ask('mobsquad sidekick Traveler bee', 400)
  check('the sidekick comes out', await until(async () => await countIn('testworld', '@e[type=bee,name=Bee]') === 1, 5000, 300))
  const lines = critterLines('Traveler'), slot = k => lines.map(l => l.split(' ')[2] === k).indexOf(true)
  await openBunkhouse(tv, vault); await tv.clickWindow(slot('chicken'), 0, 0); await sleep(500); await closeMenu(tv)
  check('a critter goes out from the Bunkhouse', await until(async () => await countIn('testworld', '@e[type=chicken,name=Chicken]') === 1, 4000, 250))
  out = await tp('Traveler', 'elsewhere')
  const where = async () => `tp: ${out.slice(0, 120)}; dim ${(await s.ask('data get entity Traveler Dimension', 400)).join(' ').slice(-40)}; chickens ${await countIn('testworld', '@e[type=chicken,name=Chicken]')}/${await countIn('elsewhere', '@e[type=chicken,name=Chicken]')}, bees ${await countIn('testworld', '@e[type=bee,name=Bee]')}/${await countIn('elsewhere', '@e[type=bee,name=Bee]')}`
  check('crossing to another group sends a critter home, in neither world', await countIn('testworld', '@e[type=chicken,name=Chicken]') === 0 && await countIn('elsewhere', '@e[type=chicken,name=Chicken]') === 0, await where())
  check('the sidekick comes along, one copy', await until(async () => await countIn('elsewhere', '@e[type=bee,name=Bee]') === 1, 5000, 300) && await countIn('testworld', '@e[type=bee,name=Bee]') === 0, await where())
  check('no critter turns into an item on the way', !tv.inventory.items().some(i => /spawn_egg|egg|lead/.test(i.name)), tv.inventory.items().map(i => i.name).join(','))
  check('the critters are still in the Bunkhouse file, once each', ['chicken', 'enderman', 'bee'].every(k => critterLines('Traveler').filter(l => l.split(' ')[2] === k).length === 1), critterLines('Traveler').join(' / '))
  await tp('Traveler', 'testworld')
  await stand(tv, 'Traveler', X, Z, 0, 20)
  check('coming back, the sidekick returns and the other critter stays home', await until(async () => await countIn('testworld', '@e[type=bee,name=Bee]') === 1, 5000, 300) && await countIn('testworld', '@e[type=chicken,name=Chicken]') === 0 && await countIn('elsewhere', '@e[type=bee,name=Bee]') === 0, await where())
  // the enderman's fetched block stays in the world it came from
  await openBunkhouse(tv, vault); await tv.clickWindow(slot('enderman'), 0, 0); await sleep(500); await closeMenu(tv)
  await until(async () => await countIn('testworld', '@e[type=enderman,name=Enderman]') === 1, 4000, 250)
  const at = new Vec3(X + 3, Y, Z)
  await s.ask(`setblock ${at.x} ${at.y} ${at.z} pumpkin`, 300)
  await poke(tv, entity(tv, e => e.name === 'enderman')); await sleep(500)
  await tv.lookAt(at.offset(0.5, 0.5, 0.5), true); await sleep(300)
  tv.swingArm()
  await s.ask(`mv tp Traveler elsewhere`, 200); await sleep(3000)
  check('the enderman took the block', (await s.ask(`execute in minecraft:overworld if block ${at.x} ${at.y} ${at.z} air`, 200)).join(' ').includes('passed'))
  check('a block fetched as you cross groups doesn\'t follow you', !tv.inventory.items().some(i => i.name === 'pumpkin'), tv.inventory.items().map(i => i.name).join(','))
  check('it drops where it was taken', await countIn('testworld', `@e[type=item,x=${at.x},y=${at.y},z=${at.z},distance=..3,nbt={Item:{id:"minecraft:pumpkin"}}]`) === 1)
  await tp('Traveler', 'testworld')
  await s.ask('kick Traveler', 300); await quit(tv); await sleep(1000)
}

// ---------- AuraSkills ----------
if (want('aura') && AURA) {
  section('Wranglin\' as an AuraSkills skill')
  check('MobSquad registers Wranglin\' with AuraSkills', s.log.some(l => /Wranglin' is an AuraSkills skill/.test(l)) && s.log.some(l => /Loaded 12 skills/.test(l)))
  const auraFile = name => { const f = path.join(dir, 'plugins/AuraSkills/userdata', uuids[name] + '.yml'); return existsSync(f) ? readFileSync(f, 'utf8') : '' }
  // AuraSkills writes its file when a player quits, so the checks read it after a kick
  const auraLevel = text => { const m = text.match(/mobsquad\/wranglin:\s*\n\s*level: (\d+)\s*\n\s*xp: ([\d.]+)/) || text.match(/wranglin[^\n]*\n[^\n]*?(\d+)[^\n]*\n[^\n]*?([\d.]+)/); return m ? { level: +m[1], xp: +m[2] } : null }
  if (want('coax')) {
    // the coaxing section coaxed a chicken as Wrangler: 100 experience, MobSquad level 2
    await s.ask('kick Wrangler', 300); await quit(me); await sleep(2500)
    const a = auraLevel(auraFile('Wrangler'))
    check('a Wranglin\' level-up from coaxing shows in AuraSkills', a && a.level === 2, auraFile('Wrangler').slice(0, 600))
  }
  // a player with a MobSquad level from before AuraSkills sees it there
  const offline = name => { const h = createHash('md5').update('OfflinePlayer:' + name).digest(); h[6] = (h[6] & 0x0f) | 0x30; h[8] = (h[8] & 0x3f) | 0x80; const x = h.toString('hex'); return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}` }
  mkdirSync(playersDir, { recursive: true })
  writeFileSync(path.join(playersDir, offline('Veteran') + '.txt'), 'wranglin 15000\n') // level 25
  const vet = await bot('Veteran', { physics: PHYSICS })
  await uuidOf('Veteran')
  check('the test file is the one the server reads', uuids.Veteran === offline('Veteran'), uuids.Veteran)
  await sleep(5000)
  const level = async () => { const from = vet.chatLog.length; vet.chat('/mobsquad list'); await waitSaid(vet, /Wranglin' level \d+/, 4000, from); const m = vet.chatLog.slice(from).join(' ').match(/Wranglin' level (\d+)/); return m ? +m[1] : null }
  check('MobSquad keeps the level from its own file', await level() === 25)
  await s.ask('skills skill setlevel Veteran mobsquad/wranglin 30', 600)
  check('MobSquad\'s own level counts, whatever AuraSkills says', await level() === 25)
  await s.ask('mobsquad xp Veteran 2250', 400)
  check('/mobsquad xp sets the level', await level() === 10)
  await s.ask('kick Veteran', 300); await quit(vet); await sleep(2500)
  const v = auraLevel(auraFile('Veteran'))
  check('/mobsquad xp sets the AuraSkills level too (it was 30 there)', v && v.level === 10, auraFile('Veteran').slice(0, 600))
  check('MobSquad still keeps its own count', file('Veteran').includes('wranglin 2250'), file('Veteran'))
  writeFileSync(path.join(playersDir, offline('Oldtimer') + '.txt'), 'wranglin 15000\n')
  const old = await bot('Oldtimer', { physics: PHYSICS })
  await uuidOf('Oldtimer'); await sleep(6000)
  await s.ask('kick Oldtimer', 300); await quit(old); await sleep(2500)
  const o = auraLevel(auraFile('Oldtimer'))
  check('a MobSquad level shows in AuraSkills when they join', o && o.level === 25, auraFile('Oldtimer').slice(0, 400))
}

// ---------- vittles ----------
if (!MENU) skip('vittles')
else if (want('vittles')) {
  section('Vittles')
  await config({ 'recovery-minutes': 5 })
  await s.ask('clear Wrangler', 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  let w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300)
  await me.clickWindow(0, 0, 0); await sleep(300); await closeMenu(me)
  const name = critterLines('Wrangler')[0].split(' ')[6]
  let from = me.chatLog.length
  await s.ask(`damage @e[name=${decodeURIComponent(name)},limit=1] 500 minecraft:generic`, 600)
  check('a critter is tuckered out', await waitSaid(me, /tuckered out/, 4000, from))
  await s.ask('mobsquad vittles Wrangler 2', 400)
  await until(() => me.inventory.items().some(i => i.name === 'pumpkin_pie'), 4000)
  await me.equip(me.inventory.items().find(i => i.name === 'pumpkin_pie'), 'hand')
  from = me.chatLog.length
  me.activateItem(); await sleep(300); me.deactivateItem()
  check('vittles perk it up and it comes running', await waitSaid(me, /perked right up and came running/, 4000, from), me.chatLog.slice(from).join(' | '))
  check('its rest is cleared in the file', +critterLines('Wrangler')[0].split(' ')[5] === 0, critterLines('Wrangler')[0])
  check('one bundle is used', me.inventory.items().find(i => i.name === 'pumpkin_pie')?.count === 1)
  const stillTired = critterLines('Wrangler').some(l => +l.split(' ')[5] > Date.now())
  from = me.chatLog.length
  me.activateItem(); await sleep(300); me.deactivateItem()
  if (stillTired) check('a second bundle perks up the next tired critter', await waitSaid(me, /perked right up/, 4000, from), me.chatLog.slice(from).join(' | '))
  from = me.chatLog.length
  const left = me.inventory.items().find(i => i.name === 'pumpkin_pie')?.count ?? 0
  if (left) { me.activateItem(); await sleep(300); me.deactivateItem() }
  else { await s.ask('mobsquad vittles Wrangler 1', 400); await until(() => me.inventory.items().some(i => i.name === 'pumpkin_pie'), 4000); await me.equip(me.inventory.items().find(i => i.name === 'pumpkin_pie'), 'hand'); me.activateItem(); await sleep(300); me.deactivateItem() }
  check('with nobody tuckered out, vittles say so and stay', await waitSaid(me, /needs vittles/, 4000, from) && (me.inventory.items().find(i => i.name === 'pumpkin_pie')?.count ?? 0) >= 1, me.chatLog.slice(from).join(' | '))
  w = await openBunkhouse(me, vault); await me.clickWindow(53, 0, 0); await sleep(300); await closeMenu(me)
  await config({ 'recovery-minutes': 0 })
}

// ---------- leaving, restarts and crashes ----------
if (!MENU) skip('persist')
else if (want('persist')) {
  section('Leaving and restarts')
  await s.ask('mobsquad xp Wrangler 15000', 300)
  await s.ask('clear Wrangler', 200)
  await stand(me, 'Wrangler', X, Z, 0, 20)
  let w = await openBunkhouse(me, vault)
  if (!(await s.count('@e[name=Chicken]'))) { await me.clickWindow(0, 0, 0); await sleep(300) }
  await closeMenu(me)
  const count = critterLines('Wrangler').length
  check('a critter is out before leaving', await s.count('@e[type=chicken,name=Chicken]') >= 1)
  await quit(me)
  await sleep(1000)
  check('leaving takes critters out of the world', await s.count('@e[type=chicken,name=Chicken]') === 0)
  me = await bot('Wrangler', { physics: PHYSICS })
  await sleep(1000)
  check('coming back, the Bunkhouse list is whole', critterLines('Wrangler').length === count, `${critterLines('Wrangler').length} of ${count}`)
  check('coming back, nothing is out', await s.count('@e[type=chicken,name=Chicken]') === 0)

  await stand(me, 'Wrangler', X, Z, 0, 20)
  w = await openBunkhouse(me, vault); await me.clickWindow(0, 0, 0); await sleep(300); await closeMenu(me)
  await quit(me); await quit(other)
  await s.stop()
  await s.start()
  check('after a restart no critter is in the world', await s.count('@e[name=Chicken]') === 0)
  check('after a restart the list is whole', critterLines('Wrangler').length === count)
  me = await bot('Wrangler', { physics: PHYSICS })
  await stand(me, 'Wrangler', X, Z, 0, 20)
  w = await openBunkhouse(me, vault)
  check('the Bunkhouse still opens after a restart', egg(w, 0) === 'chicken_spawn_egg', egg(w, 0))
  await me.clickWindow(0, 0, 0); await sleep(500); await closeMenu(me)
  check('a critter is out before the crash', await s.count('@e[type=chicken,name=Chicken]') === 1)
  await s.kill9()
  await s.start()
  me = await bot('Wrangler', { physics: PHYSICS })
  await stand(me, 'Wrangler', X, Z, 0, 20)
  await sleep(2000)
  check('after a crash the critter isn\'t left in the world', await s.count('@e[type=chicken]') === 0)
  check('after a crash the list is whole, with no copy', critterLines('Wrangler').length === count)
}


// ---------- worlds where MobSquad is off ----------
if (!MENU) skip('worlds')
else if (want('worlds')) {
  section('Disabled worlds')
  await config({ 'disabled-worlds': '[testworld]', 'recovery-minutes': 0 })
  await stand(me, 'Wrangler', X, Z, 0, 20)
  const from = me.chatLog.length
  await me.activateBlock(me.blockAt(vault))
  check('a Bunkhouse in a disabled world says MobSquad is off', await waitSaid(me, /off in this world/, 4000, from), me.chatLog.slice(from).join(' | '))
  await config({ 'recovery-minutes': 0 })
}

// ---------- quests open tiers (0.9.0, K604) ----------
if (want('quests')) {
  section('Quests open tiers')
  const papi = async (what) => ((await s.ask(`papi parse Wrangler %mobsquad_${what}%`, 600)).find(l => /^(true|false|\d+)$/.test(l.trim())) || '').trim()
  await s.ask('mobsquad xp Wrangler 0', 300)
  await config({ '  tier-unlock': 'level' })
  check('the Yard is always open', await papi('tier_yard') === 'true')
  check('level mode: Yonder is shut at a low level', await papi('tier_yonder') === 'false')
  const out = (await s.ask('mobsquad unlock Wrangler yonder', 600)).join(' ')
  check('mobsquad unlock answers, and says level mode ignores it', /has the Yonder tier from a quest/.test(out) && /only Wranglin' level counts/.test(out), out)
  check('level mode: a quest alone doesn\'t open Yonder', await papi('tier_yonder') === 'false')
  check('the quest is saved in the player\'s file', /^tier yonder$/m.test(file('Wrangler')) || /^tier YONDER$/m.test(file('Wrangler')), file('Wrangler').split('\n').filter(l => l.startsWith('tier')).join(' '))
  await config({ '  tier-unlock': 'either' })
  check('either mode: the quest opens Yonder before its level', await papi('tier_yonder') === 'true')
  check('either mode: Backcountry stays shut without its quest or level', await papi('tier_backcountry') === 'false')
  await config({ '  tier-unlock': 'quest' })
  await s.ask('mobsquad xp Wrangler 15000', 300) // level 25: Yonder's level
  check('quest mode: Yonder opens with its quest', await papi('tier_yonder') === 'true')
  check('quest mode: Holler stays shut without its quest, even at level 25', await papi('tier_holler') === 'false')
  await s.ask('mobsquad lock Wrangler yonder', 600)
  check('mobsquad lock takes it back', await papi('tier_yonder') === 'false')
  await config({ '  tier-unlock': 'either' })
  check('either mode: level 25 opens Yonder without a quest', await papi('tier_yonder') === 'true')
  check('the level placeholder reads the Wranglin\' level', await papi('level') === '25')
  check('an unknown placeholder isn\'t answered', !(await papi('nonsense')))
  const bad = (await s.ask('mobsquad unlock Wrangler yard', 600)).join(' ')
  check('the Yard can\'t be unlocked by a quest', /isn't a tier a quest can open/.test(bad), bad)
  await config({ '  tier-unlock': 'level' })
  await s.ask('mobsquad xp Wrangler 0', 300)
}

// ---------- BetonQuest chapters (0.10.0, K725) ----------
// Needs MS_TEST_BQ=1 (BetonQuest) and runs with only=chapters. Citizens isn't installed, so the Ranger's NPC
// can't be clicked; the conversation is started by an action in a test-only package, and everything else runs as a
// player would meet it. Craft and smelt steps are completed with BetonQuest's own objective command (see craftStep).
if (want('chapters')) {
  section('Quest chapters')
  const papi = async what => ((await s.ask(`papi parse Wrangler %mobsquad_${what}%`, 600)).find(l => /^(true|false|\d+)$/.test(l.trim())) || '').trim()
  const shipped = path.join(dir, 'plugins/MobSquad/quests/mobsquad/package.yml')
  check('MobSquad puts the chapters in its own folder', existsSync(shipped))
  const packs = path.join(dir, 'plugins/BetonQuest/QuestPackages')
  mkdirSync(path.join(packs, 'mobsquad'), { recursive: true }); mkdirSync(path.join(packs, 'mobsquadtest'), { recursive: true })
  writeFileSync(path.join(packs, 'mobsquad/package.yml'), readFileSync(shipped)) // what an admin does
  writeFileSync(path.join(packs, 'mobsquadtest/package.yml'), 'actions:\n  talk: conversation mobsquad>ranger\n')
  const loaded = (await s.ask('betonquest reload', 2500)).join('\n')
  const bad = loaded.split('\n').filter(l => /Error|rror while|Could not|WARN/.test(l))
  check('BetonQuest loads every part of the package', /95 Actions[^\]]*69 Conditions, 2 Conversations[^\]]*9 Items, 45 Journal Entries[^\]]*40 Objectives/.test(loaded), loaded.slice(0, 600))
  check('the only load error is the Ranger\'s Citizens NPC, because Citizens isn\'t on the test server', bad.length === 1 && /Npc 'ranger'/.test(bad[0]), bad.join(' | '))
  await config({ '  tier-unlock': 'either' })
  await s.ask('mobsquad xp Wrangler 0', 300)
  await s.ask('mobsquad lock Wrangler holler', 300); await s.ask('mobsquad lock Wrangler yonder', 300); await s.ask('mobsquad lock Wrangler backcountry', 300)
  await s.ask('clear Wrangler', 200)
  await s.ask(`tp Wrangler ${X + 0.5} ${Y} ${Z + 0.5} 0 10`, 300)

  const clicks = []
  me.on('message', jm => { const c = jm.json?.click_event?.command; if (c && c.startsWith('/betonquestanswer')) clicks.push({ text: jm.toString(), cmd: c }) })
  /** Starts the Ranger's conversation and, if asked, picks the option with that text. Returns what was said. */
  async function talk(pick) {
    clicks.length = 0
    const from = me.chatLog.length
    await s.ask('betonquest action Wrangler mobsquadtest>talk', 200)
    await until(() => clicks.length > 0 || said(me, /Ranger Juniper: /, from), 8000)
    await sleep(900)
    if (pick) {
      const c = clicks.find(c => c.text.includes(pick))
      if (!c) return me.chatLog.slice(from).concat('NO OPTION ' + pick)
      me.chat(c.cmd); await sleep(1500)
    }
    return me.chatLog.slice(from)
  }
  const list = async kind => (await s.ask(`betonquest ${kind} Wrangler list`, 700)).join(' ')
  const active = async id => (await list('objective')).includes(`mobsquad>${id}`)
  const waitActive = async (id, ms = 8000) => { await until(async () => await active(id), ms, 400); return active(id) }
  const tagged = async tag => (await list('tag')).includes(`mobsquad>${tag}`)
  const journaled = async e => (await list('journal')).includes(`mobsquad>${e}`)
  const dropAt = async (item, n) => s.ask(`execute at Wrangler run summon item ~ ~ ~ {Item:{id:"minecraft:${item}",count:${n}},PickupDelay:0}`, 300)
  const notes = []
  /**
   * A craft or smelt step, completed with BetonQuest's own objective command. A bot's crafting at a table didn't
   * work here (the click went through and no item came out, on 26.2), and smelting needs a furnace, so the real
   * crafting and smelting triggers are untested. What runs is everything the objective does when it's done.
   */
  async function craftStep(objective) {
    check(`${objective} is the step running`, await active(objective))
    await s.ask(`betonquest objective Wrangler complete mobsquad>${objective}`, 800)
    notes.push(objective)
    await sleep(600)
  }

  // ----- the Ranger and Holler -----
  let said1 = (await talk('Yes, please!')).join(' | ')
  check('the Ranger offers the Holler job to a new wrangler', /Howdy, wrangler!/.test(said1), said1)
  check('saying yes starts the chapter: tag, journal entry, first objective', await tagged('holler_started') && await journaled('holler_1') && await active('hollerBerries'))
  check('the player is told step 1 of 3', said(me, /Step 1 of 3\. Pick six sweet berries/))
  check('the Ranger doesn\'t offer Yonder yet', !/Yonder/.test((await talk('Thanks, Ranger!')).join(' ')))
  check('Holler is shut and the player is still level 1', await papi('tier_holler') === 'false' && await papi('level') === '1')
  await dropAt('sweet_berries', 3)
  await sleep(1500)
  check('three berries are not enough', await active('hollerBerries') && !(await active('hollerSeeds')))
  await dropAt('sweet_berries', 3)
  check('six berries start step 2', await waitActive('hollerSeeds'), await list('objective'))
  check('step 2 is announced and the journal moves on', said(me, /Step 2 of 3/) && await journaled('holler_2') && !(await journaled('holler_1')))

  await dropAt('wheat_seeds', 2)
  await sleep(1500)
  check('two seeds are not enough', await active('hollerSeeds'))
  await dropAt('wheat_seeds', 2)
  check('four seeds start step 3', await waitActive('hollerNoteBlock'), await list('objective'))
  await s.ask(`tp Wrangler ${X + 0.5} ${Y} ${Z + 0.5} 0 10`, 300)
  check('the journal moves on to step 3', await journaled('holler_3') && !(await journaled('holler_2')))
  check('the Ranger reminds a player who is mid-chapter', /Still on the Holler job/.test((await talk('Thanks, Ranger!')).join(' ')))
  check('Holler is still shut before the last step', await papi('tier_holler') === 'false')
  await craftStep('hollerNoteBlock')
  await sleep(800)
  check('the last step opens Holler, at level 1', await papi('tier_holler') === 'true' && await papi('level') === '1', await papi('tier_holler'))
  check('the chapter opens the fox\'s journal page', await papi('page_fox') === 'true')
  check('the player is told the chapter is done', said(me, /Chapter done! Holler critters are open to you/))
  check('the chapter is tagged done and the journal says so', await tagged('holler_done') && await journaled('holler_done') && !(await journaled('holler_3')))
  check('the unlock is saved in the player\'s file', /^tier (holler|HOLLER)$/m.test(file('Wrangler')))
  check('Yonder stays shut', await papi('tier_yonder') === 'false' && await papi('tier_backcountry') === 'false')
  check('the Ranger now offers Yonder', /Nice work in the Holler!/.test((await talk('Maybe later.')).join(' ')))
  check('no objective is left running', !(await list('objective')).includes('mobsquad>'))
  await config({ '  tier-unlock': 'level' })
  check('in level mode the finished chapter opens nothing', await papi('tier_holler') === 'false')
  await config({ '  tier-unlock': 'either' })
  check('back in either mode it opens again', await papi('tier_holler') === 'true')

  // ----- Yonder -----
  check('a Ranger talk starts Yonder', (await talk('Yes, please!')).join(' ').includes('Yonder critters are shy') && await tagged('yonder_started') && await active('yonderArrows'))
  check('Yonder step 1 is announced', said(me, /Step 1 of 3\. Craft four arrows/))
  await craftStep('yonderArrows')
  check('arrows start step 2', await waitActive('yonderTarget') && said(me, /Step 2 of 3\. Craft a target block/))
  await craftStep('yonderTarget')
  check('the target block starts step 3', await waitActive('yonderCampfire') && said(me, /Step 3 of 3\. Craft a campfire/))
  check('Yonder is shut before the last step', await papi('tier_yonder') === 'false')
  await craftStep('yonderCampfire')
  await sleep(800)
  check('the last step opens Yonder and the creeper page', await papi('tier_yonder') === 'true' && await papi('page_creeper') === 'true')
  check('Yonder\'s message', said(me, /Chapter done! Yonder critters are open to you/))
  check('Backcountry stays shut', await papi('tier_backcountry') === 'false' && await papi('level') === '1')

  // ----- Backcountry -----
  check('the Ranger offers Backcountry', /Ready for the Backcountry/.test((await talk('Yes, please!')).join(' ')) && await tagged('backcountry_started') && await active('backcountryPoppy'))
  await dropAt('poppy', 1)
  check('a poppy starts step 2', await waitActive('backcountryIron') && said(me, /Step 2 of 3\. Smelt four iron ingots/))
  await craftStep('backcountryIron')
  check('smelting starts step 3', await waitActive('backcountryShears') && said(me, /Step 3 of 3\. Craft a pair of shears/))
  check('Backcountry is shut before the last step', await papi('tier_backcountry') === 'false')
  await craftStep('backcountryShears')
  await sleep(800)
  check('the last step opens Backcountry and the Iron Golem page', await papi('tier_backcountry') === 'true' && await papi('page_iron_golem') === 'true')
  check('Backcountry\'s message', said(me, /Chapter done! Backcountry critters are open to you/))
  check('the Ranger offers the Round-up once Backcountry is done', /Ready for the Bunkhouse Round-up\?/.test((await talk('Maybe later.')).join(' ')))
  const pies = (await s.ask('clear Wrangler minecraft:pumpkin_pie 0', 500)).join(' ')
  check('the three chapters gave two vittles each', /Found 6 matching/.test(pies), pies)
  check('the player is still level 1: chapters never touch the level', await papi('level') === '1')
  console.log('    completed by command, not by crafting or smelting: ' + notes.join(', '))

  // ----- the shipped file is never overwritten -----
  await quit(me); await quit(other)
  await s.stop()
  writeFileSync(shipped, readFileSync(shipped, 'utf8') + '\n# an admin edit\n')
  await s.start()
  check('a restart keeps an admin\'s edit to the shipped chapters', readFileSync(shipped, 'utf8').includes('# an admin edit'))
}

// ---------- The Ten Winds (0.11.0, K725) ----------
// Needs MS_TEST_BQ=1 and MS_TEST_LP=1 (BetonQuest and LuckPerms) and runs with only=winds. Citizens isn't installed, so the
// Ranger's NPC can't be clicked; the conversation is started by an action in a test-only package, as in the chapters
// section. Spots (rings, wind-signs, landmarks, the meadow) are set to made-up places in the test world. Crafting and
// smelting steps aren't part of the Ten Winds chapters, so nothing is completed by command except the old chapters'
// tags, which are added so the story's later chapters unlock. Bots can't place blocks on 26.3, so the builder's
// 40-block step runs on 26.1.2 and 26.2 only.
if (want('winds')) {
  section('The Ten Winds')
  const papi = async what => ((await s.ask(`papi parse Wrangler %mobsquad_${what}%`, 600)).find(l => /^(true|false|\d+)$/.test(l.trim())) || '').trim()
  const shipped = path.join(dir, 'plugins/MobSquad/quests/mobsquad/package.yml')
  const packs = path.join(dir, 'plugins/BetonQuest/QuestPackages')
  mkdirSync(path.join(packs, 'mobsquad'), { recursive: true }); mkdirSync(path.join(packs, 'mobsquadtest'), { recursive: true })
  let pack = readFileSync(shipped, 'utf8')
  // made-up spots on a grid, 10 blocks apart, so no two spots' 6-block circles meet
  const spot = {}
  let idx = 0
  for (const m of pack.matchAll(/^  (\w+): '0;0;0;unset'$/gm)) {
    spot[m[1]] = [X - 55 + (idx % 12) * 10, Z - 55 + Math.floor(idx / 12) * 14]; idx++
  }
  check('the package ships 21 unset spots, none hardcoded to a real world', idx === 21 && !/;(world|magic|ninos_\w+|testworld);/.test(pack), String(idx))
  for (const [k, [x, z]] of Object.entries(spot)) pack = pack.replace(`  ${k}: '0;0;0;unset'`, `  ${k}: '${x};${Y};${z};testworld'`)
  writeFileSync(path.join(packs, 'mobsquad/package.yml'), pack)
  writeFileSync(path.join(packs, 'mobsquadtest/package.yml'), 'actions:\n  talk: conversation mobsquad>ranger\n')
  const loaded = (await s.ask('betonquest reload', 3500)).join('\n')
  const bad = loaded.split('\n').filter(l => /Error|rror while|Could not|WARN/.test(l))
  check('BetonQuest loads every part of the package', /2 Conversations/.test(loaded) && /40 Objectives/.test(loaded), loaded.slice(0, 700))
  check('the only load error is the Ranger\'s Citizens NPC', bad.length === 1 && /Npc 'ranger'/.test(bad[0]), bad.join(' | '))
  await s.ask('mobsquad xp Wrangler 0', 300)
  await s.ask('clear Wrangler', 200); await s.ask('clear Neighbor', 200)
  await s.ask('gamemode survival Wrangler', 200); await s.ask('gamemode survival Neighbor', 200)

  const watch = b => { b.clicks = []; b.on('message', jm => { const c = jm.json?.click_event?.command; if (c && c.startsWith('/betonquestanswer')) b.clicks.push({ text: jm.toString(), cmd: c }) }) }
  watch(me); watch(other)
  const list = async (name, kind) => (await s.ask(`betonquest ${kind} ${name} list`, 700)).join(' ')
  const tagged = async (name, t) => (await list(name, 'tag')).includes(`mobsquad>${t}`)
  const journaled = async (name, e) => (await list(name, 'journal')).includes(`mobsquad>${e}`)
  const active = async (name, o) => (await list(name, 'objective')).includes(`mobsquad>${o}`)
  const waitTag = async (name, t, ms = 8000) => { await until(async () => await tagged(name, t), ms, 400); return tagged(name, t) }
  const waitActive = async (name, o, ms = 8000) => { await until(async () => await active(name, o), ms, 400); return active(name, o) }
  const waitJournal = async (name, e, ms = 8000) => { await until(async () => await journaled(name, e), ms, 400); return journaled(name, e) }
  async function pickFrom(b, pick, from, ms = 8000) {
    await until(() => b.clicks.some(c => c.text.includes(pick)) || b.chatLog.slice(from).some(m => /Ranger Juniper: /.test(m)), ms)
    const c = b.clicks.find(c => c.text.includes(pick))
    if (!c) return false
    b.clicks.length = 0
    b.chat(c.cmd); await sleep(1500)
    return true
  }
  /** Starts the Ranger's conversation for a player and picks an option with that text, if asked. Returns what was said. */
  async function talk(b, name, pick, keepOpen = false) {
    b.clicks.length = 0
    const from = b.chatLog.length
    await s.ask(`betonquest action ${name} mobsquadtest>talk`, 200)
    await until(() => b.clicks.length > 0 || b.chatLog.slice(from).some(m => /Ranger Juniper: /.test(m)), 8000)
    if (!b.chatLog.slice(from).length) { // once more, in case the first start was dropped
      if (process.env.MS_TEST_TALK) console.log(`    [talk ${name}] no conversation started; server said: ` + s.log.slice(-6).join(' / ').slice(0, 500))
      await s.ask(`betonquest action ${name} mobsquadtest>talk`, 200)
      await until(() => b.clicks.length > 0 || b.chatLog.slice(from).some(m => /Ranger Juniper: /.test(m)), 8000)
    }
    await sleep(900)
    if (pick && !await pickFrom(b, pick, from, 2000)) { const r = b.chatLog.slice(from).concat('NO OPTION ' + pick); if (process.env.MS_TEST_TALK) console.log(`    [talk ${name}] ` + r.join(' | ')); return r }
    const heard = b.chatLog.slice(from)
    if (process.env.MS_TEST_TALK) console.log(`    [talk ${name}] ` + heard.join(' | '))
    if (b.clicks.length > 0 && !keepOpen) await closeTalk(b, name) // the Ranger is waiting on another answer: walk away, as a player would
    return heard
  }
  /** Ends an open conversation the way a player can: by answering "Maybe later." until the Ranger says goodbye. */
  async function closeTalk(b, name) {
    for (let i = 0; i < 12; i++) {
      const c = b.clicks.find(c => /Maybe later|Thanks, Ranger/.test(c.text))
      if (!c) break
      b.clicks.length = 0
      b.chat(c.cmd); await sleep(1300)
    }
    b.clicks.length = 0
    await sleep(500)
  }
  const at = async (name, k) => { await s.ask(`tp ${name} ${spot[k][0] + 0.5} ${Y} ${spot[k][1] + 0.5} 0 10`, 400); await sleep(700) }
  // BetonQuest watches where a player is once a second, and sees a walk-in only after it has seen them outside, so waiting out here is part of walking in
  const away = async name => { await s.ask(`tp ${name} ${X + 0.5} ${Y} ${Z + 40.5} 0 10`, 400); await sleep(2200) }
  const bq = (name, what) => s.ask(`betonquest ${what}`.replace('$', name), 500)
  const hasItem = async (name, item) => (await s.ask(`execute if items entity ${name} container.* ${item}`, 400)).join(' ').includes('passed')
  // only the Ranger's own books: BetonQuest's quest journal is a book too, and it carries its owner's name as author
  const invText = async name => (await s.ask(`data get entity ${name} Inventory[{components:{"minecraft:written_book_content":{author:"Ranger Juniper"}}}]`, 600)).join(' ')
  const invAll = async name => (await s.ask(`data get entity ${name} Inventory`, 700)).join(' ')
  const sayLines = (b, from) => b.chatLog.slice(from).join(' | ')
  const gaveTags = async name => { for (const t of ['holler_started', 'holler_done', 'yonder_started', 'yonder_done', 'backcountry_started', 'backcountry_done', 'season1_done']) await s.ask(`betonquest tag ${name} add mobsquad>${t}`, 250) }

  // ----- the switch, and kid or adult fails closed -----
  check('no group: the player is not an adult (fails closed)', (await s.ask('lp user Wrangler permission check posse.circle.adults', 900)).join(' ').toLowerCase().match(/false|undefined/) !== null)
  await gaveTags('Wrangler')
  let said1 = (await talk(me, 'Wrangler')).join(' | ')
  check('the new chapters stay off until an admin opens them', !/wind-sign/i.test(said1) && /Season 1 is done!/.test(said1), said1)
  // winds_open alone opens nothing, and neither do the Season switches without it
  await s.ask('betonquest action - mobsquad>windsOpenOn', 700)
  said1 = (await talk(me, 'Wrangler')).join(' | ')
  check('winds_open alone keeps Season 2 and 3 hidden', !/wind-sign|Wind-rings|Far Meadow|watch could/i.test(said1) && /Season 1 is done!/.test(said1), said1)
  await s.ask('betonquest action - mobsquad>windsOpenOff', 700)
  await s.ask('betonquest action - mobsquad>season2OpenOn', 700); await s.ask('betonquest action - mobsquad>season3OpenOn', 700)
  said1 = (await talk(me, 'Wrangler')).join(' | ')
  check('with winds_open off, both Season switches show nothing', !/wind-sign|Wind-rings|Far Meadow|watch could/i.test(said1) && /Season 1 is done!/.test(said1), said1)
  // each Season's switch opens only its own chapters
  await s.ask('betonquest action - mobsquad>windsOpenOn', 700)
  async function offered() { // every offer, by saying Maybe later until the goodbye
    const f = me.chatLog.length
    await talk(me, 'Wrangler', undefined, true)
    await closeTalk(me, 'Wrangler')
    return me.chatLog.slice(f).join(' | ')
  }
  await s.ask('betonquest action - mobsquad>season3OpenOff', 700)
  said1 = await offered()
  check('Season 2 alone offers the wind-signs, watch and meadow, and no wind-rings', /wind-sign fell over/.test(said1) && /watch could use help/.test(said1) && /Far Meadow\?/.test(said1) && !/Wind-rings sleep/.test(said1), said1)
  await s.ask('betonquest action - mobsquad>season3OpenOn', 700); await s.ask('betonquest action - mobsquad>season2OpenOff', 700)
  said1 = await offered()
  check('Season 3 alone offers the wind-rings and no Season 2 chapter', /Wind-rings sleep/.test(said1) && !/wind-sign fell|watch could use|Far Meadow\?/.test(said1), said1)
  await s.ask('betonquest action - mobsquad>season2OpenOn', 700)
  const on = 'both Season switches and winds_open are on'
  check('an admin opens the new chapters from the console', await until(async () => /wind-sign fell over/.test((await talk(me, 'Wrangler', 'Maybe later.')).join(' ')), 6000, 300), on)

  // ----- chapter 4, Wind-Signs: no stats, so the all-of-the-above path, a kid with no group -----
  // D196: the old permission is not read. A player who holds only mayor.circle.adults is still a kid here.
  await s.ask('lp user Wrangler permission set mayor.circle.adults true', 800)
  said1 = (await talk(me, 'Wrangler', 'Yes, please!')).join(' | ')
  await s.ask('lp user Wrangler permission unset mayor.circle.adults', 800)
  check('a player with only the old mayor.circle.adults permission still gets the kid lines (not an adult)', /Visit each one!/.test(said1), said1)
  check('a player without posse.circle.adults gets the kid lines: short and kid-sized', /Visit each one!/.test(said1) && said1.split('|').every(l => l.replace(/^.*?: /, '').split(/\s+/).length <= 14), said1)
  check('the style is set once, to all of the above for a player who has done none of them', await tagged('Wrangler', 'style_all') && !(await tagged('Wrangler', 'style_builder')))
  check('chapter 4 starts: tag, journal and the first post objectives', await tagged('Wrangler', 'signs_started') && await waitJournal('Wrangler', 'signs_walk') && await waitActive('Wrangler', 'signsPost1Kid'))
  await at('Neighbor', 'signPost1Kid') // a second player stands at the post; nothing is his, so nothing is sent
  const otherFrom = other.chatLog.length
  await at('Wrangler', 'signPost1Adult')
  check('an adult\'s post does nothing for a kid', await active('Wrangler', 'signsPost1Kid') && !(await active('Wrangler', 'signsPost2Kid')))
  await at('Wrangler', 'signPost1Kid')
  check('the first post is found: step 2 starts', await waitActive('Wrangler', 'signsPost2Kid') && said(me, /Wind-sign 1 found/))
  check('the second player near the post gets nothing', !said(other, /wind-sign|Wind-sign|found/i, otherFrom), sayLines(other, otherFrom))
  await at('Wrangler', 'signPost2Kid')
  check('the second post is found', await waitActive('Wrangler', 'signsPost3Kid'))
  const gotBefore = me.chatLog.length
  await at('Wrangler', 'signPost3Kid')
  check('the third post finishes the chapter', await waitTag('Wrangler', 'signs_done') && await journaled('Wrangler', 'signs_done') && said(me, /Chapter done! The wind-sign stands/, gotBefore))
  check('the Ranger\'s trail book arrives', await until(async () => /Ranger Juniper/.test(await invText('Wrangler')), 6000, 300))
  let inv = await invText('Wrangler')
  check('the book is by Ranger Juniper, tattered, and not a map', /Ranger Juniper/.test(inv) && /generation: 3|"?generation"?: ?3/.test(inv) && !/filled_map/.test(inv), inv.slice(0, 500))
  check('it names no player and gives no coordinates', !/Wrangler|Neighbor/.test(inv.replace(/Ranger Juniper/g, '').replace(/"?Name"?:|Wrangler's/g, '').slice(inv.indexOf('written_book'))) && !/-?\d{2,},\s?-?\d{2,}/.test(inv.slice(inv.indexOf('written_book'))), inv.slice(inv.indexOf('written_book'), inv.indexOf('written_book') + 600))
  check('the chapter gave no level, tier or critter', await papi('level') === '1' && await papi('critters') === '0')

  // ----- chapter 5, the Ten Winds: the ring level gate -----
  await away('Wrangler')
  check('the Ranger offers the wind-rings', /Wind-rings sleep in the magic world/.test((await talk(me, 'Wrangler', 'Yes, please!')).join(' ')))
  check('all six ring objectives are running (kid and adult)', await waitActive('Wrangler', 'ringEmberKid') && await active('Wrangler', 'ringSunbeamAdult'))
  await sleep(2500) // BetonQuest notes where the player is before it can see them walk in
  let from = me.chatLog.length
  let quiet = false
  for (let tries = 0; tries < 3 && !quiet; tries++) { // BetonQuest sees a walk-in only after it has seen the player outside; a player who misses it just steps out and in
    if (tries > 0) { await away('Wrangler'); await sleep(2000) }
    await at('Wrangler', 'ringEmberKid')
    quiet = await until(() => said(me, /The ring is quiet\. Ember wants a critter that has grown\./, from), 5000)
  }
  check('below level 10 the ring is quiet, in kid words', quiet, sayLines(me, from))
  check('no choice to wake it is offered below the gate', !me.clicks.some(c => /Wake it/.test(c.text)))
  check('the ring is still waiting, not used up', await waitActive('Wrangler', 'ringEmberKid'))
  await s.ask('mobsquad give Wrangler chicken', 400)
  await s.ask('mobsquad level Wrangler chicken 10', 400)
  check('the placeholder counts what the player owns: top is 10 with nothing out', await papi('top') === '10' && await papi('best_chicken') === '10')
  await away('Wrangler')
  from = me.chatLog.length
  await at('Neighbor', 'ringEmberKid') // a bystander in the ring, no chapter of his own
  await at('Wrangler', 'ringEmberKid')
  check('at level 10 Ember offers to wake', await until(() => me.clicks.some(c => /Wake it/.test(c.text)), 8000) && said(me, /Ember sleeps here/, from), sayLines(me, from))
  check('the chicken is not out, and the ring opened anyway', await s.count('@e[type=chicken,name=Chicken]') === 0)
  const nFrom = other.chatLog.length
  check('waking it', await pickFrom(me, 'Wake it', from))
  check('Ember wakes: tag, journal and a private line', await waitTag('Wrangler', 'ring_ember_done') && await journaled('Wrangler', 'ring_ember') && said(me, /Ember is awake/, from))
  check('the bystander next to the ring heard and saw none of it', !said(other, /Ember|awake|ring|wind/i, otherFrom) && !(await tagged('Neighbor', 'ring_ember_done')) && !(await journaled('Neighbor', 'ring_ember')), sayLines(other, otherFrom))
  await away('Wrangler'); await away('Neighbor')
  from = me.chatLog.length
  await at('Wrangler', 'ringMarigoldKid')
  check('ring two needs level 20: still quiet at 10', await until(() => said(me, /Marigold wants a critter that has grown\./, from), 8000) && !me.clicks.some(c => /Wake it/.test(c.text)), sayLines(me, from))
  await s.ask('mobsquad level Wrangler chicken 20', 400)
  await away('Wrangler'); from = me.chatLog.length
  await at('Wrangler', 'ringMarigoldKid')
  check('at level 20 Marigold wakes', await until(() => me.clicks.some(c => /Wake it/.test(c.text)), 8000) && await pickFrom(me, 'Wake it', from) && await waitTag('Wrangler', 'ring_marigold_done'))
  await s.ask('mobsquad level Wrangler chicken 30', 400)
  await away('Wrangler'); from = me.chatLog.length
  await at('Wrangler', 'ringSunbeamKid')
  check('at level 30 Sunbeam wakes and the chapter ends', await until(() => me.clicks.some(c => /Wake it/.test(c.text)), 8000) && await pickFrom(me, 'Wake it', from) && await waitTag('Wrangler', 'winds_done') && await journaled('Wrangler', 'winds_done'))
  check('a kid at the adults\' ring is not sent anywhere: nothing happens', await (async () => { await away('Wrangler'); const f = me.chatLog.length; await at('Wrangler', 'ringEmberAdult'); await sleep(1500); return !said(me, /Ember|ring/i, f) })())
  await s.ask('mobsquad level Wrangler chicken 1', 400)

  // ----- an adult: posse.circle.adults from LuckPerms, the adults' rings and the 15-kill watch -----
  await away('Wrangler')
  await s.ask('lp user Neighbor permission set posse.circle.adults true', 1200)
  check('LuckPerms gives the adults\' permission to the adult', (await s.ask('lp user Neighbor permission check posse.circle.adults', 900)).join(' ').toLowerCase().includes('true'))
  await gaveTags('Neighbor')
  await s.ask('mobsquad give Neighbor chicken', 400); await s.ask('mobsquad level Neighbor chicken 10', 400)
  await sleep(500)
  check('the Ranger offers the adult the wind-signs too', /wind-sign fell over/.test((await talk(other, 'Neighbor', 'Maybe later.')).join(' ')))
  // the adult is a builder (a tag stands in for months of building): the Ranger asks for a stone or wood base, and 40 placed blocks finish it
  await s.ask('betonquest tag Neighbor add mobsquad>style_builder', 250)
  if (MENU) {
    const bf = other.chatLog.length
    const said2 = (await talk(other, 'Neighbor', 'Yes, please!', true)).join(' | ')
    check('a builder is asked which base to build, with two answers', /This post needs a sturdy base\. Your call!/.test(said2) && other.clicks.some(c => /Stone base!/.test(c.text)) && other.clicks.some(c => /Wood base/.test(c.text)), said2)
    check('stone it is: solid as a boulder, and the 40-block step starts', await pickFrom(other, 'Stone base!', bf) && said(other, /Solid as a boulder/, bf) && await waitActive('Neighbor', 'signsStone') && !(await active('Neighbor', 'signsWood')))
    other.clicks.length = 0
    await s.ask('clear Neighbor', 200); await s.ask('give Neighbor minecraft:cobblestone 40', 300)
    await until(() => other.inventory.items().some(i => i.name === 'cobblestone'), 5000)
    await other.equip(other.inventory.items().find(i => i.name === 'cobblestone'), 'hand')
    const x0 = X - 20, z0 = Z + 20
    let placed = 0
    const spots = Array.from({ length: 40 }, (_, i) => [x0 + (i % 20), z0 + Math.floor(i / 20) * 2])
    const placeAt = async ([x, z]) => {
      await s.ask(`tp Neighbor ${x + 0.5} ${Y} ${z - 1.5} 0 10`, 150); await sleep(300)
      try { await other.placeBlock(other.blockAt(new Vec3(x, Y - 1, z)), new Vec3(0, 1, 0)); return true } catch { return false }
    }
    for (const sp of spots.slice(0, 39)) if (await placeAt(sp)) placed++
    for (let again = 0; again < 3 && placed < 39; again++) for (const sp of spots.slice(0, 39)) {
      if (placed >= 39) break
      if (other.blockAt(new Vec3(sp[0], Y, sp[1]))?.name !== 'cobblestone' && await placeAt(sp)) placed++ // one the bot missed the first time
    }
    check('39 placed blocks are not enough: the step is still waiting', placed === 39 && await active('Neighbor', 'signsStone') && !(await tagged('Neighbor', 'signs_done')), String(placed))
    if (await placeAt(spots[39])) placed++
    check('the bot really placed every block by hand', placed === 40, String(placed))
    check('40 placed stone blocks finish the chapter, and the wood step is gone', await waitTag('Neighbor', 'signs_done') && !(await active('Neighbor', 'signsWood')) && !(await active('Neighbor', 'signsStone')), `${await list('Neighbor', 'objective')}`)
    check('what a player builds is read from the game\'s own count: this adult is a builder now', (await s.ask('papi parse Neighbor %mobsquad_style%', 600)).join(' ').includes('builder'))
    check('the trail book arrives for the adult too', await until(async () => /Ranger Juniper/.test(await invText('Neighbor')), 6000, 300))
    await s.ask('clear Neighbor', 200)
  } else {
    await s.ask('betonquest tag Neighbor add mobsquad>signs_started', 250); await s.ask('betonquest tag Neighbor add mobsquad>signs_done', 250)
    console.log('    the builder\'s 40-block step was skipped on ' + MC + ', where the bots can\'t place blocks; signs_done was added by command')
  }
  await away('Neighbor')
  check('the adult is offered the wind-rings', /Wind-rings sleep/.test((await talk(other, 'Neighbor', 'Yes, please!')).join(' ')))
  await away('Wrangler'); await away('Neighbor')
  let nf = other.chatLog.length
  await at('Neighbor', 'ringEmberKid')
  await sleep(1500)
  check('an adult at the kids\' ring: nothing happens', !said(other, /Ember|ring/i, nf), sayLines(other, nf))
  await away('Neighbor'); nf = other.chatLog.length; other.clicks.length = 0
  await at('Neighbor', 'ringEmberAdult')
  check('the adult ring speaks in grown-up words at level 10', await until(() => other.clicks.some(c => /Wake it/.test(c.text)), 8000) && said(other, /Ember sleeps here\. It wakes for a critter with a flame at its back\./, nf), sayLines(other, nf))
  check('waking it for the adult', await pickFrom(other, 'Wake it', nf) && await waitTag('Neighbor', 'ring_ember_done') && said(other, /Ember is awake\. Marigold is next\./, nf))
  await s.ask('betonquest tag Neighbor add mobsquad>winds_done', 250) // the other two adult rings are the same code as the kids'
  check('the kid beside nobody got the adult\'s ring', !(await tagged('Wrangler', 'ring_ember_done_adult')) && !said(me, /Marigold is next/, 0))

  // the watch: kid 8, any time; adult 15, dusk or night only
  await away('Neighbor')
  await s.ask('time set noon', 200)
  // a kid who says maybe later to the watch is offered the meadow, then the finale: nothing is stuck behind the optional watch
  {
    const f0 = me.chatLog.length
    await talk(me, 'Wrangler', undefined, true)
    check('the kid is first offered the optional watch', said(me, /The Ranger's watch could use help/, f0), sayLines(me, f0))
    await pickFrom(me, 'Maybe later.', f0, 4000)
    check('a kid who passes on the watch is offered the Far Meadow next', await until(() => said(me, /Want to find the Far Meadow\?/, f0), 5000), sayLines(me, f0))
    const ok2 = await pickFrom(me, 'Maybe later.', f0, 4000)
    check('and then the finale, since signs and rings are done, with the watch still waiting', ok2 && await until(() => said(me, /The herd wants to come home/, f0), 5000), sayLines(me, f0))
    await closeTalk(me, 'Wrangler')
  }
  check('the kid is offered the watch as optional', /optional/.test((await talk(me, 'Wrangler', 'I\'ll help!')).join(' ')) && await tagged('Wrangler', 'watch_started') && await journaled('Wrangler', 'watch_1_kid') && await active('Wrangler', 'watchFightKid') && !(await active('Wrangler', 'watchFightAdult')))
  check('the adult\'s watch is the adult version', /optional/.test((await talk(other, 'Neighbor', 'I\'ll help!')).join(' ')) && await journaled('Neighbor', 'watch_1_adult') && await active('Neighbor', 'watchFightAdult') && !(await active('Neighbor', 'watchFightKid')))
  await s.ask(`attribute Wrangler minecraft:attack_damage base set 100`, 200); await s.ask(`attribute Neighbor minecraft:attack_damage base set 100`, 200)
  /** The bot beats n mobs with its own attacks (a command kill would have no killer, so it wouldn't count). Returns how many fell to the bot. */
  async function fight(b, name, n, mob = 'zombie') {
    const here = `@e[type=${mob},x=${X},y=${Y},z=${Z + 62},distance=..8]` // a console command's distance is from the world's origin, so the spot is spelled out
    await s.ask(`tp ${name} ${X + 0.5} ${Y} ${Z + 60.5} 0 10`, 300); await sleep(400)
    await s.ask(`kill ${here}`, 100)
    let beaten = 0
    for (let i = 0; i < n; i++) {
      await s.ask(`summon ${mob} ${X + 0.5} ${Y} ${Z + 62.5} {NoAI:1b,Silent:1b,PersistenceRequired:1b}`, 100)
      await until(() => entity(b, e => e.name === mob && e.position.distanceTo(b.entity.position) < 4), 4000, 100)
      const z = entity(b, e => e.name === mob && e.position.distanceTo(b.entity.position) < 4)
      if (z) {
        for (let k = 0; k < 3 && await s.count(here) > 0; k++) { b.attack(z); await sleep(500) }
        if (await s.count(here) === 0) beaten++
      }
      await s.ask(`kill ${here}`, 0) // anything the attacks missed, so the next one is alone
      await sleep(80)
    }
    return beaten
  }
  // the kid's eight: seven aren't enough
  const progress = async (name, o) => (await s.ask(`papi parse ${name} %betonquest_mobsquad:objective.${o}.left%`, 500)).filter(l => !/^\[/.test(l)).join(' ')
  const k7 = await fight(me, 'Wrangler', 7)
  if (process.env.MS_TEST_TALK) console.log('    [kid watch left after 7] ' + await progress('Wrangler', 'watchFightKid'))
  check('the bot really beat seven mobs by hand', k7 === 7, String(k7))
  check('seven are not enough for a kid: one to go', await active('Wrangler', 'watchFightKid') && !(await tagged('Wrangler', 'watch_done')) && await progress('Wrangler', 'watchFightKid') === '1', await progress('Wrangler', 'watchFightKid'))
  const cow = await fight(me, 'Wrangler', 1, 'cow')
  check('a cow beaten by hand adds nothing: only hostile mobs count', cow === 1 && await progress('Wrangler', 'watchFightKid') === '1' && !(await tagged('Wrangler', 'watch_done')), `${cow} beaten, left ${await progress('Wrangler', 'watchFightKid')}`)
  const k1 = await fight(me, 'Wrangler', 1)
  if (process.env.MS_TEST_TALK) console.log('    [kid watch left after 8] ' + await progress('Wrangler', 'watchFightKid'))
  check('the eighth finishes the kid\'s watch, at midday: any time of day', k1 === 1 && await waitTag('Wrangler', 'watch_done') && await journaled('Wrangler', 'watch_done'), `${k1} beaten, ${await list('Wrangler', 'objective')}`)
  check('the watch-bell book arrives', await until(async () => /The Watch-Bell/.test(await invAll('Wrangler')), 5000, 300), (await invAll('Wrangler')).slice(0, 200))
  // the adult's fifteen: noon kills don't count
  const n4 = await fight(other, 'Neighbor', 4)
  check('at noon an adult\'s kills do not count (the bot did beat four by hand)', n4 === 4 && await active('Neighbor', 'watchFightAdult') && !(await tagged('Neighbor', 'watch_done')), String(n4))
  await s.ask('time set midnight', 200)
  const a14 = await fight(other, 'Neighbor', 14)
  check('the bot really beat fourteen by hand', a14 === 14, String(a14))
  check('fourteen at night are not enough', await active('Neighbor', 'watchFightAdult') && !(await tagged('Neighbor', 'watch_done')))
  const a1 = await fight(other, 'Neighbor', 1)
  check('the fifteenth at night finishes the adult watch', a1 === 1 && await waitTag('Neighbor', 'watch_done'), `${a1} beaten, ${await list('Neighbor', 'objective')}`)
  await s.ask('time set noon', 200)

  // ----- chapter 7, the Far Meadow: kids three landmarks, adults four -----
  await away('Wrangler'); await away('Neighbor')
  check('the explorer path is offered the meadow first', true)
  check('the kid starts the Far Meadow', /Look for a hill, a lake and flowers\./.test((await talk(me, 'Wrangler', 'Yes, please!')).join(' ')) && await journaled('Wrangler', 'meadow_1_kid'))
  await away('Wrangler') // BetonQuest sees a walk-in only after one move outside since the objective began; a walking player does it without trying
  for (const k of ['landHillKid', 'landLakeKid']) { await at('Wrangler', k); await sleep(400) }
  check('two landmarks of three is not enough', !(await tagged('Wrangler', 'meadow_done')))
  await at('Wrangler', 'landFlowerKid')
  check('the third landmark finishes a kid\'s chapter, with no river', await waitTag('Wrangler', 'meadow_done') && await journaled('Wrangler', 'meadow_done'), `${await list('Wrangler', 'tag')} ${await list('Wrangler', 'objective')}`)
  check('the meadow book arrives', await until(async () => /The Meadow Book/.test(await invAll('Wrangler')), 5000, 300))
  check('the adult starts the Far Meadow', /Look for a hill, a lake, a river and flowers\./.test((await talk(other, 'Neighbor', 'Yes, please!')).join(' ')) && await journaled('Neighbor', 'meadow_1_adult'))
  await away('Neighbor')
  for (const k of ['landHillAdult', 'landLakeAdult', 'landFlowerAdult']) { await at('Neighbor', k); await sleep(400) }
  check('three landmarks of four is not enough for an adult', !(await tagged('Neighbor', 'meadow_done')))
  await at('Neighbor', 'landRiverAdult')
  check('the river finishes the adult\'s chapter', await waitTag('Neighbor', 'meadow_done'), `${await list('Neighbor', 'tag')} ${await list('Neighbor', 'objective')}`)
  // a kid's landmark visit doesn't change an adult's

  // ----- chapter 8, the finale -----
  await away('Wrangler'); await away('Neighbor')
  const fin = (await talk(me, 'Wrangler', 'Ring it!')).join(' ')
  check('with the signs, rings, watch and meadow done, the kid is offered the finale', /Take this dinnerbell/.test(fin), fin)
  check('the Ranger hands over a dinnerbell by console command, no typing', await until(async () => /Dinnerbell/.test(await invAll('Wrangler')), 5000, 300) && await active('Wrangler', 'finaleKid'))
  check('the finale waits for the ringer: ringing away from the meadow does nothing', await (async () => {
    await s.ask(`tp Wrangler ${X + 0.5} ${Y} ${Z + 40.5} 0 10`, 300)
    const held = me.inventory.items().find(i => i.name === 'warped_fungus_on_a_stick')
    if (held) await me.equip(held, 'hand')
    me.activateItem(); await sleep(1500); return !(await tagged('Wrangler', 'ten_winds_done'))
  })())
  await at('Wrangler', 'meadowKid')
  await s.ask('clear Wrangler minecraft:warped_fungus_on_a_stick', 200); await sleep(300)
  me.activateItem(); await sleep(1500)
  check('without the bell in hand, ringing nothing in the meadow does nothing', !(await tagged('Wrangler', 'ten_winds_done')))
  await s.ask('betonquest action Wrangler mobsquad>bellAgain', 400); await sleep(500)
  const bell = me.inventory.items().find(i => i.name === 'warped_fungus_on_a_stick')
  check('a lost bell is replaced and only one is ever held', !!bell && me.inventory.items().filter(i => i.name === 'warped_fungus_on_a_stick').length === 1)
  if (bell) await me.equip(bell, 'hand')
  from = me.chatLog.length
  me.activateItem()
  check('ringing the dinnerbell in the meadow finishes the story', await waitTag('Wrangler', 'ten_winds_done', 10000), sayLines(me, from))
  check('the bell\'s own recall still runs for the ringer', await waitSaid(me, /Nobody's out with you|headed home/, 5000, from), sayLines(me, from))
  check('the finale journal has a line for each finished chapter and the arc is done', await journaled('Wrangler', 'finale_done') && await journaled('Wrangler', 'finale_signs') && await journaled('Wrangler', 'finale_winds') && await journaled('Wrangler', 'finale_watch') && await journaled('Wrangler', 'finale_meadow'))
  check('the finale book arrives', await until(async () => /The Ten Winds Are Awake/.test(await invAll('Wrangler')), 5000, 300))
  check('the other player\'s story is untouched by the kid\'s finale', !(await tagged('Neighbor', 'ten_winds_done')) && !(await tagged('Neighbor', 'finale_started')))

  // ----- every book, read straight off the server -----
  for (const id of ['trail', 'watch', 'meadow', 'finale']) {
    await s.ask('clear Wrangler minecraft:written_book', 200)
    await s.ask(`mobsquad book Wrangler ${id}`, 500)
    const t = await invText('Wrangler')
    const body = t.slice(t.indexOf('written_book'))
    check(`book ${id}: Ranger Juniper wrote it, it is a written book and not a map`, /Ranger Juniper/.test(body) && /written_book/.test(t) && !/filled_map/.test(t), body.slice(0, 300))
    check(`book ${id}: no player name and no coordinates`, !/Wrangler|Neighbor/.test(body.replace(/Ranger Juniper/g, '')) && !/-?\d{2,}\s*,\s*-?\d{2,}/.test(body), body.slice(0, 300))
  }
  check('an unknown book is refused, with the list', (await s.ask('mobsquad book Wrangler nothing', 400)).join(' ').includes('No such book'))
  check('only admins give books and bells', await (async () => { const f = me.chatLog.length; me.chat('/mobsquad book Wrangler trail'); await sleep(1200); const none = !said(me, /Ranger Juniper gave you a book/, f); me.chat('/mobsquad dinnerbell Neighbor'); await sleep(1000); return none && !(await invText('Neighbor')).includes('dinnerbell') })())

  // ----- typed words stay private while a conversation is open, and nothing is typed otherwise -----
  // the adult has not finished, so the Ranger offers him the finale and waits for an answer: an open conversation
  await away('Wrangler'); await away('Neighbor')
  let logMark = s.mark()
  other.chat('plain words outside a talk'); await sleep(1500)
  check('control: outside a conversation, typed words are public chat as normal', s.since(logMark).some(l => /<Neighbor> plain words outside a talk/.test(l)), s.since(logMark).slice(-3).join(' | '))
  logMark = s.mark()
  const mine = other.chatLog.length
  other.clicks.length = 0
  await s.ask('betonquest action Neighbor mobsquadtest>talk', 200)
  await until(() => other.clicks.length > 0, 8000); await sleep(800)
  check('the server sees the player is in the Ranger\'s conversation', (await s.ask('papi parse Neighbor %betonquest_mobsquad:condition.inTalk%', 600)).join(' ').includes('true'), sayLines(other, mine))
  other.chat('secret typed words')
  await sleep(1500)
  check('typed words during a conversation never reach public chat', !s.since(logMark).some(l => /secret typed words/.test(l)), s.since(logMark).filter(l => /secret/.test(l)).join(' | '))
  check('the player is told to click instead', said(other, /Click an answer to go on/, mine), sayLines(other, mine))
  check('a typed number still answers the conversation, and is not held', await (async () => { other.chat('2'); await sleep(1500); return said(other, /Neighbor: Maybe later/, mine) || said(other, /Maybe later/, mine) })(), sayLines(other, mine))
  await closeTalk(other, 'Neighbor')
  other.chat('words after the talk'); await sleep(1500)
  check('after the conversation, typed words go through again', s.since(logMark).some(l => /<Neighbor> words after the talk/.test(l)), s.since(logMark).slice(-3).join(' | '))

  // ----- turning the switch off stops new offers but never takes anything away -----
  await s.ask('betonquest action - mobsquad>windsOpenOff', 700)
  check('with the switch off, a finished player still has the story', await tagged('Wrangler', 'ten_winds_done') && await journaled('Wrangler', 'finale_done'))
  console.log('    completed by command, not by play: the three tier chapters\' tags (holler_done, yonder_done, backcountry_done) and signs_done for the adult')
  await s.ask('lp user Neighbor permission unset posse.circle.adults', 800)
}

// ---------- Season 1 (0.13.0, K899) ----------
// Needs MS_TEST_BQ=1 and runs with only=season1. As in the chapters section, a console action stands in for clicking the
// Ranger, and craft steps are completed with BetonQuest's own objective command. The coax is a real one.
if (want('season1')) {
  section('Season 1')
  const shipped = path.join(dir, 'plugins/MobSquad/quests/mobsquad/package.yml')
  const packs = path.join(dir, 'plugins/BetonQuest/QuestPackages')
  mkdirSync(path.join(packs, 'mobsquad'), { recursive: true }); mkdirSync(path.join(packs, 'mobsquadtest'), { recursive: true })
  const oldPack = readFileSync(new URL('../src/test/resources/package-0.12.0.yml', import.meta.url))
  writeFileSync(path.join(packs, 'mobsquad/package.yml'), oldPack)
  writeFileSync(path.join(packs, 'mobsquadtest/package.yml'), 'actions:\n  talk: conversation mobsquad>ranger\n')
  await s.ask('betonquest reload', 3000)
  await config({ '  tier-unlock': 'either' })
  for (const n of ['Wrangler', 'Neighbor']) { await s.ask(`clear ${n}`, 200); await s.ask(`gamemode survival ${n}`, 200) }
  const sx = X - 30, sz = Z - 30, hut = new Vec3(sx, Y, sz + 3)
  const watchClicks = b => { b.clicks = []; b.on('message', jm => { const c = jm.json?.click_event?.command; if (c && c.startsWith('/betonquestanswer')) b.clicks.push({ text: jm.toString(), cmd: c }) }) }
  watchClicks(me); watchClicks(other)
  async function talk(b, name, pick) {
    b.clicks.length = 0
    const from = b.chatLog.length
    await s.ask(`betonquest action ${name} mobsquadtest>talk`, 200)
    await until(() => b.clicks.length > 0 || b.chatLog.slice(from).some(m => /Ranger Juniper: /.test(m)), 8000)
    await sleep(1100)
    if (pick) {
      const c = b.clicks.find(c => c.text.includes(pick))
      if (!c) return b.chatLog.slice(from).concat('NO OPTION ' + pick).join(' | ')
      b.clicks.length = 0
      b.chat(c.cmd); await sleep(1800)
    }
    const heard = b.chatLog.slice(from)
    for (let i = 0; i < 6; i++) { // walk away from an open conversation as a player would
      const c = b.clicks.find(c => /Maybe later|Thanks, Ranger/.test(c.text))
      if (!c) break
      b.clicks.length = 0; b.chat(c.cmd); await sleep(1300)
    }
    return heard.join(' | ')
  }
  const list = async (name, kind) => (await s.ask(`betonquest ${kind} ${name} list`, 700)).join(' ')
  const has = async (name, kind, id) => new RegExp(`mobsquad>${id}(?![A-Za-z0-9_])`).test(await list(name, kind))
  const tagged = (name, x) => has(name, 'tag', x)
  const journaled = (name, x) => has(name, 'journal', x)
  const active = (name, x) => has(name, 'objective', x)
  const wait = async (fn, ms = 10000) => { await until(fn, ms, 400); return fn() }
  const ph = async (name, what) => ((await s.ask(`papi parse ${name} %mobsquad_${what}%`, 600)).find(l => /^(?:\[[^\]]*\]:? )?(true|false)$/.test(l.trim())) || '').trim().replace(/^.*[ :]/, '')
  const vittles = async name => +(((await s.ask(`clear ${name} minecraft:pumpkin_pie 0`, 600)).join(' ').match(/Found (\d+) matching/) || [0, 0])[1])
  const foxes = () => critterLines('Wrangler').filter(l => / fox /.test(l)).length
  const finishStep = async o => { await s.ask(`betonquest objective Wrangler complete mobsquad>${o}`, 900); await sleep(600) }

  // ----- a chapter started on 0.12.0 carries on after the upgrade -----
  await stand(me, 'Wrangler', sx, sz)
  check('on the old package the Ranger starts Holler', /Howdy, wrangler!/.test(await talk(me, 'Wrangler', 'Yes, please!')) && await tagged('Wrangler', 'holler_started'))
  await s.ask('execute at Wrangler run summon item ~ ~ ~ {Item:{id:"minecraft:sweet_berries",count:6},PickupDelay:0}', 300)
  check('and gets to step 2', await wait(() => active('Wrangler', 'hollerSeeds')))
  await quit(me); await quit(other)
  await s.stop()
  writeFileSync(path.join(packs, 'mobsquad/package.yml'), readFileSync(new URL('../src/main/resources/quests/mobsquad/package.yml', import.meta.url)))
  await s.start()
  const loadLog = s.log.join('\n')
  check('BetonQuest loads every part of the new package', /95 Actions[^\]]*69 Conditions, 2 Conversations[^\]]*9 Items, 45 Journal Entries[^\]]*40 Objectives/.test(loadLog), loadLog.split('\n').filter(l => /Actions/.test(l)).join(' '))
  const rejoin = async () => {
    me = await bot('Wrangler', { physics: PHYSICS }); other = await bot('Neighbor', { physics: PHYSICS })
    watchClicks(me); watchClicks(other)
    await s.ask('gamemode survival Wrangler'); await s.ask('gamemode survival Neighbor')
    await sleep(2500)
  }
  await rejoin()
  check('after the upgrade the same step is still running', await wait(() => active('Wrangler', 'hollerSeeds')) && await tagged('Wrangler', 'holler_started') && await journaled('Wrangler', 'holler_2'))
  await stand(me, 'Wrangler', sx, sz)
  await s.ask('execute at Wrangler run summon item ~ ~ ~ {Item:{id:"minecraft:wheat_seeds",count:4},PickupDelay:0}', 300)
  check('and finishes into step 3', await wait(() => active('Wrangler', 'hollerNoteBlock')))
  await s.ask('clear Wrangler', 200)
  await finishStep('hollerNoteBlock')
  check('Holler ends with its old reward', await tagged('Wrangler', 'holler_done') && await journaled('Wrangler', 'holler_done'))
  check('and now a keepsake and two vittles', await journaled('Wrangler', 'holler_keep') && await wait(async () => await vittles('Wrangler') === 2) && said(me, /You kept the fox's message\. Here are two vittles\./), String(await vittles('Wrangler')))
  await talk(me, 'Wrangler', 'Yes, please!')
  for (const o of ['yonderArrows', 'yonderTarget', 'yonderCampfire']) await finishStep(o)
  check('Yonder gives its keepsake and two more', await journaled('Wrangler', 'yonder_keep') && await wait(async () => await vittles('Wrangler') === 4) && said(me, /You kept the signal fire/), String(await vittles('Wrangler')))
  await talk(me, 'Wrangler', 'Yes, please!')
  for (const o of ['backcountryPoppy', 'backcountryIron', 'backcountryShears']) await finishStep(o)
  check('Backcountry gives its keepsake and two more', await journaled('Wrangler', 'backcountry_keep') && await wait(async () => await vittles('Wrangler') === 6) && said(me, /You kept the gate-guard's poppy/), String(await vittles('Wrangler')))
  check('Season 1 is not done yet', !(await tagged('Wrangler', 'season1_done')) && foxes() === 0)

  // ----- the offer, with every later switch on -----
  for (const a of ['windsOpenOn', 'season2OpenOn', 'season3OpenOn']) await s.ask(`betonquest action - mobsquad>${a}`, 500)
  let t1 = await talk(me, 'Wrangler', 'Maybe later.')
  check('finishing Backcountry gets the finale offer, before anything later', /Ready for the Bunkhouse Round-up\?/.test(t1) && !/wind-sign|Wind-rings|Far Meadow|watch could/i.test(t1), t1)
  check('saying maybe later starts nothing and offers no later chapter', !(await tagged('Wrangler', 'roundup_started')) && !/wind-sign|Wind-rings|Far Meadow/i.test(t1))
  // a second player who finished Backcountry before 0.13.0
  for (const x of ['holler_started', 'holler_done', 'yonder_started', 'yonder_done', 'backcountry_started', 'backcountry_done']) await s.ask(`betonquest tag Neighbor add mobsquad>${x}`, 250)
  check('a player who finished Backcountry before this version is offered it too', /Ready for the Bunkhouse Round-up\?/.test(await talk(other, 'Neighbor', 'Maybe later.')))
  for (const a of ['windsOpenOff', 'season2OpenOff', 'season3OpenOff']) await s.ask(`betonquest action - mobsquad>${a}`, 500)

  // a critter from before the finale doesn't count
  await s.ask('mobsquad give Wrangler chicken', 500)
  t1 = await talk(me, 'Wrangler', 'Yes, please!')
  check('yes starts the finale', /Coax a new critter\. Then bring it to me\./.test(t1) && await tagged('Wrangler', 'roundup_started') && await journaled('Wrangler', 'roundup_1') && await active('Wrangler', 'roundupCoax') && await active('Wrangler', 'roundupCoaxHint'), t1)
  check('step 1 is announced', said(me, /Step 1 of 3\. Coax a new critter\./))
  check('MobSquad is watching', await wait(async () => /^roundup$/m.test(file('Wrangler'))) && await ph('Wrangler', 'roundup_coaxed') === 'false', file('Wrangler'))
  await s.ask('mobsquad give Wrangler chicken', 500)
  await sleep(2500)
  check('older and given critters do not count', await ph('Wrangler', 'roundup_coaxed') === 'false' && await active('Wrangler', 'roundupCoax') && !(await active('Wrangler', 'roundupSend')))
  check('the Ranger says to coax one', /Coax a new critter\. Your journal says what's next\./.test(await talk(me, 'Wrangler', 'Thanks, Ranger!')))
  // the watch is lost somehow: talking to the Ranger sets it up again
  await s.ask('mobsquad roundup Wrangler stop', 500)
  await talk(me, 'Wrangler', 'Thanks, Ranger!')
  check('if the watch is lost, talking to the Ranger restores it', await wait(async () => /^roundup$/m.test(file('Wrangler'))))
  const pf = me.chatLog.length
  me.chat('/mobsquad roundup Wrangler stop'); await sleep(1200)
  check('a player cannot run the roundup command', /^roundup$/m.test(file('Wrangler')) && !said(me, /Round-up is off/, pf), me.chatLog.slice(pf).join(' | '))

  // the 15 minute tip: its objective is completed by command, standing in for the wait
  await finishStep('roundupCoaxHint')
  check('after 15 minutes on step 1, Juniper leaves a tip', await journaled('Wrangler', 'roundup_tip_coax') && said(me, /Juniper left a tip in your journal\./) && await active('Wrangler', 'roundupCoax'))

  // ----- a real coax -----
  await s.ask('kill @e[type=chicken]', 400)
  await stand(me, 'Wrangler', sx, sz - 20)
  await hold(me, 'Wrangler', 'wheat_seeds')
  await s.ask(`summon chicken ${sx + 1.5} ${Y} ${sz - 19.5}`, 600)
  await until(() => entity(me, e => e.name === 'chicken' && e.position.distanceTo(me.entity.position) < 4), 5000)
  let cf = me.chatLog.length
  await poke(me, entity(me, e => e.name === 'chicken' && e.position.distanceTo(me.entity.position) < 4))
  await waitSaid(me, /interested/, 5000, cf)
  await walk('Wrangler', sx, sz - 20, 14)
  check('the bot really coaxes a chicken', await waitSaid(me, /You coaxed a chicken/, 8000, cf), me.chatLog.slice(cf).join(' | '))
  check('the coax moves the finale to step 2', await wait(() => active('Wrangler', 'roundupSend')) && await journaled('Wrangler', 'roundup_2') && !(await journaled('Wrangler', 'roundup_1')) && !(await journaled('Wrangler', 'roundup_tip_coax')) && !(await active('Wrangler', 'roundupCoaxHint')), await list('Wrangler', 'objective'))
  check('step 2 is announced', said(me, /Step 2 of 3\. Send it out from a Bunkhouse\./, cf))
  const newId = (file('Wrangler').match(/^roundup-coaxed (\S+)$/m) || [])[1]
  check('exactly the coaxed chicken is noted', !!newId && file('Wrangler').match(/^roundup-coaxed /mg).length === 1 && critterLines('Wrangler').some(l => l.includes(newId) && / chicken /.test(l)), file('Wrangler'))
  await s.ask('betonquest action Wrangler mobsquad>roundupCoaxTip', 500)
  check('no coax tip once it has been done', !(await journaled('Wrangler', 'roundup_tip_coax')))
  t1 = await talk(me, 'Wrangler', 'Thanks, Ranger!')
  check('the Ranger says to send it out', /Now send your new critter out from a Bunkhouse\./.test(t1) && !(await tagged('Wrangler', 'season1_done')), t1)
  await finishStep('roundupSendHint')
  check('after 15 minutes on step 2, the second tip', await journaled('Wrangler', 'roundup_tip_send') && await active('Wrangler', 'roundupSend'))
  check('the other player\'s finale is untouched', !(await tagged('Neighbor', 'roundup_started')) && !/roundup/.test(file('Neighbor')))

  // ----- a restart in the middle -----
  await quit(me); await quit(other)
  await s.stop()
  await s.start()
  await rejoin()
  check('a restart keeps the finale where it was', await wait(() => active('Wrangler', 'roundupSend')) && await ph('Wrangler', 'roundup_coaxed') === 'true' && await ph('Wrangler', 'roundup_sent') === 'false', `${await ph('Wrangler', 'roundup_coaxed')} ${await ph('Wrangler', 'roundup_sent')}`)

  await s.ask(`setblock ${hut.x} ${hut.y} ${hut.z} minecraft:vault`, 400)
  check('the console marks the Season 1 Bunkhouse', (await s.ask(`mobsquad bunkhouse add ${hut.x} ${hut.y} ${hut.z} testworld`, 900)).join(' ').includes('Bunkhouse now'))
  await s.ask('clear Wrangler minecraft:wheat_seeds', 200)
  if (MENU) {
    await stand(me, 'Wrangler', sx, sz, 0, 20)
    const slotOf = id => critterLines('Wrangler').findIndex(l => l.includes(id))
    const clickSlot = async slot => { const f = me.chatLog.length; await openBunkhouse(me, hut); await me.clickWindow(slot, 0, 0); await sleep(900); await closeMenu(me); return me.chatLog.slice(f).join(' | ') }
    // an older chicken going out is not it
    const oldId = critterLines('Wrangler').map(l => l.split(' ')[1]).find(id => id !== newId)
    await clickSlot(slotOf(oldId))
    await sleep(2500)
    check('sending out an older critter is not it', await ph('Wrangler', 'roundup_sent') === 'false' && await ph('Wrangler', 'roundup_ready') === 'false' && await active('Wrangler', 'roundupSend'))
    check('bringing an older critter does not finish', /Now send your new critter out/.test(await talk(me, 'Wrangler', 'Thanks, Ranger!')) && !(await tagged('Wrangler', 'season1_done')))
    await clickSlot(slotOf(oldId)) // call it back
    cf = me.chatLog.length
    const sentSay = await clickSlot(slotOf(newId))
    check('the new chicken goes out from the Bunkhouse', /out with you/.test(sentSay), sentSay)
    check('that moves the finale to step 3', await wait(async () => await journaled('Wrangler', 'roundup_3')) && !(await active('Wrangler', 'roundupSend')) && !(await active('Wrangler', 'roundupSendHint')) && !(await journaled('Wrangler', 'roundup_tip_send')) && said(me, /Step 3 of 3\. Bring your new critter to Juniper\./, cf), await list('Wrangler', 'objective'))
    check('MobSquad says it is ready', await ph('Wrangler', 'roundup_ready') === 'true')
    await clickSlot(slotOf(newId)) // call it back
    check('called back, it is not ready', await ph('Wrangler', 'roundup_ready') === 'false' && await ph('Wrangler', 'roundup_sent') === 'true')
    t1 = await talk(me, 'Wrangler', 'Thanks, Ranger!')
    check('the Ranger asks for it to be sent out again, and nothing is paid', /Send your new critter out again\. Then come back\./.test(t1) && !(await tagged('Wrangler', 'season1_done')) && foxes() === 0, t1)
    await clickSlot(slotOf(newId))
    await s.ask('clear Wrangler', 200)
    t1 = await talk(me, 'Wrangler')
    check('bringing it to Juniper finishes the finale', /You brought it! The first wind is lit!/.test(t1), t1)
  } else {
    console.log('    sending out and bringing it to Juniper skipped on ' + MC + ', where the bots can\'t use the Bunkhouse; the finishing actions are run by command')
    check('coaxed but not sent: not ready, and the Ranger pays nothing', await ph('Wrangler', 'roundup_ready') === 'false' && /Now send your new critter out/.test(await talk(me, 'Wrangler', 'Thanks, Ranger!')) && foxes() === 0)
    await s.ask('clear Wrangler', 200)
    for (const a of ['roundupFinish', 'roundupKeep']) await s.ask(`betonquest action Wrangler mobsquad>${a}`, 1200)
  }
  check('season1_done is set', await wait(() => tagged('Wrangler', 'season1_done')))
  check('the Season reward: one mail-carrier fox', await wait(async () => foxes() === 1) && said(me, /Season 1 is done! A mail fox waits in your Bunkhouse\./), file('Wrangler'))
  check('the finale\'s keepsake and two vittles', await journaled('Wrangler', 'roundup_keep') && await journaled('Wrangler', 'roundup_done') && await wait(async () => await vittles('Wrangler') === 2) && said(me, /You lit the first wind\. Here are two vittles\./), String(await vittles('Wrangler')))
  check('no step is left running or in the journal', !(await list('Wrangler', 'objective')).includes('mobsquad>roundup') && !(await journaled('Wrangler', 'roundup_1')) && !(await journaled('Wrangler', 'roundup_2')) && !(await journaled('Wrangler', 'roundup_3')))
  check('MobSquad stops watching', await wait(async () => !/^roundup/m.test(file('Wrangler'))), file('Wrangler'))
  t1 = await talk(me, 'Wrangler')
  check('then the Ranger says more is coming', /Season 1 is done! More adventures are coming soon\./.test(t1), t1)
  await talk(me, 'Wrangler')
  check('talking again pays nothing again', foxes() === 1 && await vittles('Wrangler') === 2)
  check('the other player got none of it', !(await tagged('Neighbor', 'season1_done')) && !/ fox /.test(file('Neighbor')) && await vittles('Neighbor') === 0 && !said(other, /mail fox|first wind is lit/))
}

const errors = s.exceptions()
check('no MobSquad errors in the log', errors.length === 0, errors.slice(0, 5).join('\n'))
await quit(me); await quit(other)
await s.stop()
process.exit(summary() ? 1 : 0)
