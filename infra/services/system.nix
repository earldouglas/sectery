{
  domain,
  hostName,
  modulesPath,
}:
{

  imports = [ (modulesPath + "/profiles/qemu-guest.nix") ];

  system.stateVersion = "25.05";

  boot.loader.grub.enable = true;
  boot.loader.grub.device = "/dev/sda";

  boot.initrd.availableKernelModules = [
    "ahci"
    "xhci_pci"
    "virtio_pci"
    "virtio_scsi"
    "sd_mod"
    "sr_mod"
  ];

  fileSystems."/" = {
    device = "/dev/disk/by-label/root";
    fsType = "ext4";
  };

  fileSystems."/boot" = {
    device = "/dev/disk/by-label/BOOT";
    fsType = "vfat";
    options = [
      "fmask=0022"
      "dmask=0022"
    ];
  };

  nixpkgs.hostPlatform = "x86_64-linux";

  nix.gc.automatic = true;
  nix.gc.options = "-d";
  nix.optimise.automatic = true;

  networking.domain = domain;
  networking.hostName = hostName;

  documentation.enable = false;
  documentation.dev.enable = false;
  documentation.doc.enable = false;
  documentation.info.enable = false;
  documentation.man.enable = false;
  documentation.nixos.enable = false;

  services.fail2ban.enable = true;

  services.openssh.enable = true;

}
