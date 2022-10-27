<div x-data={}>
    <x-table>
        <x-slot name="head">
            <x-table.heading sortable>{{ __('Application') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Recipe') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Settings') }}</x-table.heading>
            <x-table.heading sortable>{{ __('Action') }}</x-table.heading>
        </x-slot>
        <x-slot name="body">
            @forelse ($runs as $run)
                <x-table.row class="border-b">
                    <x-table.cell>
                        <div class="flex flex-col">
                            <label class="font-bold whitespace-nowrap">
                                {{ $run->application->name }}
                            </label>
                        </div>
                    </x-table.cell>
                    <x-table.cell>
                        <div class="flex flex-col">
                            <label class="font-bold whitespace-nowrap">
                                {{ $run->recipe->name }}
                            </label>
                        </div>
                    </x-table.cell>
                    <x-table.cell class="w-full">
                        {{ $run->settings }}
                    </x-table.cell>
                    <x-table.cell>
                        <div class="flex flex-row space-x-2">
                            <x-button wire:click="run({{ $run->id }})" type="button" color="green">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-play class="w-3 font-green-700" />
                                    <label>Run</label>
                                </div>
                            </x-button>
                            <x-button type="button"
                                x-on:click="$wire.delete({{ $run->id }}); $dispatch('delete-alert', {id: {{ $run->id }}})"
                                color="red">
                                <div class="flex flex-row space-x-2 place-items-center">
                                    <x-icon-delete class="w-3 font-red-700" />
                                    <label>Delete</label>
                                </div>
                            </x-button>
                        </div>
                    </x-table.cell>
                </x-table.row>
            @empty
                <x-table.row class="border-b">
                    <x-table.cell colspan="7">
                        <div class="flex justify-center font-semibold text-gray-700">
                            {{ __('Nothing to display') }}
                        </div>
                    </x-table.cell>
                </x-table.row>
            @endforelse
        </x-slot>
    </x-table>
</div>
