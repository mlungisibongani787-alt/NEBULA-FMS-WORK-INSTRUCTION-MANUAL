using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.WebSockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using Symbol.RFID.SDK;
using Symbol.RFID.SDK.Domain.Reader;

namespace RFIDSystem.WindowsBridge
{
    internal static class Program
    {
        private static readonly ConcurrentDictionary<Guid, WebSocket> Clients = new ConcurrentDictionary<Guid, WebSocket>();
        private static readonly JavaScriptSerializer Json = new JavaScriptSerializer();
        private static IRfidReader _reader;
        private static IRemoteReaderManagement _management;
        private static HttpListener _listener;

        static void Main()
        {
            Console.Title = "RFID SYSTEM — RFD8500 Windows Bridge";
            _listener = new HttpListener();
            _listener.Prefixes.Add("http://127.0.0.1:8787/rfid/");
            _listener.Start();
            Console.WriteLine("RFID SYSTEM bridge listening on ws://127.0.0.1:8787/rfid/");
            Console.WriteLine("Pair the RFD8500 with Windows, then use Devices > Connect Bridge in the dashboard.");
            Task.Run(DiscoverReaders);
            while (_listener.IsListening)
            {
                try
                {
                    var ctx = _listener.GetContext();
                    if (!ctx.Request.IsWebSocketRequest)
                    {
                        ctx.Response.StatusCode = 200;
                        var bytes = Encoding.UTF8.GetBytes("RFID SYSTEM Windows Bridge is running. Use WebSocket /rfid/.");
                        ctx.Response.OutputStream.Write(bytes, 0, bytes.Length); ctx.Response.Close(); continue;
                    }
                    Task.Run(() => Accept(ctx));
                }
                catch { if (!_listener.IsListening) break; }
            }
        }

        private static async Task Accept(HttpListenerContext ctx)
        {
            WebSocketContext wsctx;
            try { wsctx = await ctx.AcceptWebSocketAsync(null); }
            catch { ctx.Response.StatusCode = 500; ctx.Response.Close(); return; }
            var id = Guid.NewGuid(); Clients[id] = wsctx.WebSocket;
            await Send(wsctx.WebSocket, new { type="status", state="online", message="Windows RFID bridge connected" });
            await PublishReaders();
            var buffer = new byte[8192];
            try
            {
                while (wsctx.WebSocket.State == WebSocketState.Open)
                {
                    var result = await wsctx.WebSocket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
                    if (result.MessageType == WebSocketMessageType.Close) break;
                    var text = Encoding.UTF8.GetString(buffer, 0, result.Count);
                    HandleMessage(text);
                }
            }
            catch { }
            finally { WebSocket ignored; Clients.TryRemove(id, out ignored); try { wsctx.WebSocket.Dispose(); } catch { } }
        }

        private static void HandleMessage(string text)
        {
            try
            {
                var root = Json.Deserialize<Dictionary<string, object>>(text);
                if (!root.TryGetValue("type", out var type) || Convert.ToString(type) != "command") return;
                var command = root.TryGetValue("command", out var c) ? Convert.ToString(c) : "";
                var payload = root.TryGetValue("payload", out var p) ? p as Dictionary<string, object> : null;
                Task.Run(() => Execute(command, payload));
            }
            catch (Exception ex) { Broadcast(new { type="status", state="error", message="Bridge command error: "+ex.Message }); }
        }

        private static void Execute(string command, Dictionary<string, object> payload)
        {
            try
            {
                if (command == "listReaders") { DiscoverReaders(); return; }
                EnsureReader();
                switch (command)
                {
                    case "inventoryStart": _reader.Inventory.Perform(); Broadcast(new { type="status", state="online", message="RFD8500 inventory started" }); break;
                    case "inventoryStop": _reader.Inventory.Stop(); Broadcast(new { type="status", state="online", message="RFD8500 inventory stopped" }); break;
                    case "locate": _reader.TagLocate.Perform(Convert.ToString(payload?["epc"])); Broadcast(new { type="status", state="online", message="RFD8500 tag locate started" }); break;
                    case "stopLocate": _reader.TagLocate.Stop(); Broadcast(new { type="status", state="online", message="RFD8500 tag locate stopped" }); break;
                    case "setBeeper": SetBeeper(Convert.ToString(payload?["level"])); break;
                    case "setPower": SetPower(Convert.ToUInt16(payload?["power"] ?? 2200)); break;
                }
            }
            catch (Exception ex) { Broadcast(new { type="status", state="error", message=ex.Message }); }
        }

        private static void DiscoverReaders()
        {
            try
            {
                _management = RfidSdk.ReaderManagementServicesFactory.Create(ReaderCommunicationMode.Bluetooth);
                var infos = _management.GetReaders(ReaderSearchOptions.AllReaders);
                var list = new List<object>();
                foreach (var info in infos)
                {
                    var reader = RfidSdk.RfidReaderFactory.Create(info);
                    list.Add(new { name=reader.FriendlyName, model=reader.Capabilities.ModelName, connected=reader.IsConnected, id=info.ID });
                }
                Broadcast(new { type="readers", readers=list });
                var first = infos.FirstOrDefault();
                if (first != null && _reader == null)
                {
                    _reader = RfidSdk.RfidReaderFactory.Create(first);
                    ConfigureReaderEvents();
                }
            }
            catch (Exception ex) { Broadcast(new { type="status", state="error", message="Reader discovery failed: "+ex.Message }); }
        }

        private static void EnsureReader()
        {
            if (_reader == null) DiscoverReaders();
            if (_reader == null) throw new InvalidOperationException("No paired RFD8500 was found in Windows Bluetooth devices.");
            if (!_reader.IsConnected) { _reader.Connect(); ConfigureReaderEvents(); }
        }

        private static void ConfigureReaderEvents()
        {
            if (_reader == null) return;
            _reader.Inventory.TagDataReceived -= OnTag;
            _reader.Inventory.TagDataReceived += OnTag;
            _reader.Inventory.InventoryStarted -= OnInventoryStarted;
            _reader.Inventory.InventoryStarted += OnInventoryStarted;
            _reader.Inventory.InventoryStopped -= OnInventoryStopped;
            _reader.Inventory.InventoryStopped += OnInventoryStopped;
            _reader.TagLocate.ProximityPercentReceived -= OnProximity;
            _reader.TagLocate.ProximityPercentReceived += OnProximity;
        }

        private static void OnTag(object sender, TagDataReceivedEventArgs e)
        {
            Broadcast(new { type="tag", reader=_reader?.FriendlyName ?? "RFD8500", tag=new { epc=e.EPCId, rssi=e.RSSI, reads=e.TagSeenCount } });
        }
        private static void OnProximity(object sender, ProximityPercentReceivedEventArgs e)
        {
            Broadcast(new { type="locate", data=new { relativeDistance=e.ProximityPercent } });
        }
        private static void OnInventoryStarted(object sender, EventArgs e) { Broadcast(new { type="status", state="online", message="Inventory started" }); }
        private static void OnInventoryStopped(object sender, EventArgs e) { Broadcast(new { type="status", state="online", message="Inventory stopped" }); }

        private static void SetBeeper(string level)
        {
            BEEPER_VOLUME v = BEEPER_VOLUME.MEDIUM_BEEP;
            switch ((level ?? "MEDIUM").ToUpperInvariant()) { case "HIGH": v=BEEPER_VOLUME.HIGH_BEEP; break; case "LOW": v=BEEPER_VOLUME.LOW_BEEP; break; case "QUIET": v=BEEPER_VOLUME.QUIET_BEEP; break; }
            _reader.Configurations.BeeperVolume = v;
            Broadcast(new { type="status", state="online", message="Beeper set to "+level });
        }
        private static void SetPower(ushort requested)
        {
            ushort antennaId=0; var cfg=_reader.Configurations.Antennas[antennaId].Configuration; cfg.TransmitPowerIndex=requested; _reader.Configurations.Antennas[antennaId].Configuration=cfg;
            Broadcast(new { type="status", state="online", message="Transmit power updated" });
        }

        private static async Task PublishReaders()
        {
            try
            {
                if (_management == null) _management=RfidSdk.ReaderManagementServicesFactory.Create(ReaderCommunicationMode.Bluetooth);
                var infos=_management.GetReaders(ReaderSearchOptions.AllReaders); var list=new List<object>();
                foreach(var info in infos){var r=RfidSdk.RfidReaderFactory.Create(info);list.Add(new{name=r.FriendlyName,model=r.Capabilities.ModelName,connected=r.IsConnected,id=info.ID});}
                await BroadcastAsync(new {type="readers",readers=list});
            }catch{}
        }

        private static void Broadcast(object message) => _ = BroadcastAsync(message);
        private static async Task BroadcastAsync(object message)
        {
            var data=Encoding.UTF8.GetBytes(Json.Serialize(message));
            foreach(var kv in Clients.ToArray())
            {
                try{if(kv.Value.State==WebSocketState.Open)await kv.Value.SendAsync(new ArraySegment<byte>(data),WebSocketMessageType.Text,true,CancellationToken.None);}catch{}
            }
        }
        private static async Task Send(WebSocket ws, object message){try{var data=Encoding.UTF8.GetBytes(Json.Serialize(message));await ws.SendAsync(new ArraySegment<byte>(data),WebSocketMessageType.Text,true,CancellationToken.None);}catch{}}
    }
}
