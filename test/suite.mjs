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
import { existsSync, readdirSync, readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import { Vec3 } from 'vec3'
import { bot, check, DEFAULT_CONFIG, freshServer, jar, quit, section, Server, sleep, summary, until } from './harness.mjs'

const MC = process.argv[2]
if (!['26.1.2', '26.2', '26.3'].includes(MC)) throw new Error('usage: node suite.mjs <26.1.2|26.2|26.3> [only=a,b]')
const ONLY = (process.argv.find(a => a.startsWith('only=')) || '').slice(5).split(',').filter(Boolean)
const want = n => ONLY.length === 0 || ONLY.includes(n)
const PHYSICS = MC !== '26.3'
const MENU = PHYSICS
const skip = name => { if (want(name)) console.log(`\n# ${name}: skipped on ${MC}, where the bots can't use a block`) }
const Y = -60

const dir = freshServer('suite-' + MC, { paper: jar(`paper-${MC}.jar`),
  plugins: [jar('ViaVersion.jar'), jar('ViaBackwards.jar'), jar('GriefPrevention.jar')], props: { difficulty: 'easy' } })
const s = new Server({ dir })
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
const uuids = {}
async function uuidOf(name) {
  const out = (await s.ask(`data get entity ${name} UUID`, 400)).join(' ')
  const m = out.match(/\[I; (-?\d+), (-?\d+), (-?\d+), (-?\d+)\]/)
  if (!m) throw new Error('no UUID for ' + name + ': ' + out)
  const hex = m.slice(1).map(n => (Number(n) >>> 0).toString(16).padStart(8, '0')).join('')
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
  check('leaving the chicken behind ends the trail', await waitSaid(me, /lost the trail/, 4000, from), me.chatLog.slice(from).join(' | '))
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
  await s.ask('kill @e[type=chicken]', 300)
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
  await s.ask('kill @e[type=chicken]', 300)
}

// ---------- sending out and following ----------
if (!MENU) skip('posse')
else if (want('posse')) {
  section('Sending critters out')
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

const errors = s.exceptions()
check('no MobSquad errors in the log', errors.length === 0, errors.slice(0, 5).join('\n'))
await quit(me); await quit(other)
await s.stop()
process.exit(summary() ? 1 : 0)
